package com.letschat.mvp_1.Controllers;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.letschat.mvp_1.Models.ScheduleMessage;
import com.letschat.mvp_1.Service.ScheduleService;

import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;


@CrossOrigin(originPatterns="*")
@RestController
@RequestMapping("/api/schedule")
public class ScheduleMessageController {
    private ScheduleService scheduleService;
    public ScheduleMessageController(ScheduleService scheduleRepo){
        this.scheduleService=scheduleRepo;
    }

    @PostMapping("/create")
    public Mono<String> schedule(@RequestBody ScheduleMessage msg) {
        return scheduleService.schedule(msg)
        .flatMap(res->Mono.just(res));
    }

    @GetMapping("/get/{chatid}")
    public Mono<List<ScheduleMessage>> getmsg(@PathVariable String chatid,@RequestHeader("user-id") String userid){
        return scheduleService.getmsg(userid, chatid).collectList();
    }

    @PostMapping("/delete/{id}")
    public Mono<String> delete(@PathVariable Long id) {
        return scheduleService.delete(id)
        .flatMap(res->Mono.just(res));
    }
    
}
