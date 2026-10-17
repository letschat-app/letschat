package com.letschat.mvp_1.Service;

import com.letschat.mvp_1.WebSocket.MyWebSocketHandler;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import com.letschat.mvp_1.Models.ScheduleMessage;
import com.letschat.mvp_1.Repositories.ScheduleRepo;

import reactor.core.publisher.Flux;
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
        if (msg.getTime() == null) {
            return Mono.error(new IllegalArgumentException("Scheduled time cannot be null"));
        }

        // Align/snap strictly to allowed 15-minute intervals: 0, 15, 30, 45 (:00, :15, :30, :45)
        LocalDateTime t = msg.getTime();
        int min = t.getMinute();
        int rem = min % 15;
        if (rem != 0 || t.getSecond() != 0 || t.getNano() != 0) {
            t = t.plusMinutes(15 - rem).withSecond(0).withNano(0);
        }

        LocalDateTime now = LocalDateTime.now();
        if (t.isBefore(now)) {
            int nowRem = now.getMinute() % 15;
            t = now.plusMinutes(15 - nowRem).withSecond(0).withNano(0);
        }
        msg.setTime(t);

        System.out.println("[SCHEDULED MSG] Scheduled for 15-min interval slot: " + msg.getTime());
        return scheduleRepo.save(msg)
        .flatMap(sch->Mono.just("success"))
        .switchIfEmpty(Mono.just("failed"));
    }

    @Scheduled(cron = "0 0,15,30,45 * * * ?")
    public void process0() {
        System.out.println("[SCHEDULED MSG] Running 15-minute scheduled messages batch at :00, :15, :30, :45");
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

    public Flux<ScheduleMessage> getmsg(String userid ,String chatid){
        return scheduleRepo.findAllByChatIdAndSenderId(chatid,userid);
    }

    public Mono<String> delete(Long id){
        return scheduleRepo.deleteById(id).thenReturn("deleted");
    }
}
