package com.letschat.mvp_1.Controllers;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.letschat.mvp_1.Models.ScheduleMessage;
import com.letschat.mvp_1.Service.ScheduleService;

import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


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
    
}
