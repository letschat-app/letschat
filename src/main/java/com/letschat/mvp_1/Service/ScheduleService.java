package com.letschat.mvp_1.Service;

import com.letschat.mvp_1.WebSocket.MyWebSocketHandler;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.letschat.mvp_1.Models.ScheduleMessage;
import com.letschat.mvp_1.Repositories.ScheduleRepo;

import reactor.core.publisher.Mono;
@Component
public class ScheduleService {
    private final MyWebSocketHandler myWebSocketHandler;
    private ScheduleRepo scheduleRepo;
    public ScheduleService(ScheduleRepo scheduleRepo, MyWebSocketHandler myWebSocketHandler){
        this.scheduleRepo=scheduleRepo;
        this.myWebSocketHandler = myWebSocketHandler;
    }

    public Mono<String> schedule(ScheduleMessage msg){
        return scheduleRepo.save(msg)
        .flatMap(sch->Mono.just("success"))
        .switchIfEmpty(Mono.just("failed"));
    }

    // @Scheduled(cron="0 * * * * *")
    // public void process(){
    //     scheduleRepo.findMesages()
    //     .flatMap(msg->myWebSocketHandler.send(msg).then(scheduleRepo.deleteById(msg.getMessageId()))).subscribe();
    // }

    @Scheduled(cron = "0 * * * * *")
    public void process0() {
        scheduleRepo.findMesages()
        .flatMap(msg ->
            myWebSocketHandler
                .send(msg)
                .then(scheduleRepo.deleteById(msg.getMessageId()))
                .onErrorResume(e -> {
                    System.out.println("Error processing message: " + msg.getMessageId());
                    return Mono.empty();
                })
        , 16) 
        .subscribe();
    }
}
