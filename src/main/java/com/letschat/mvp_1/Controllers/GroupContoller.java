package com.letschat.mvp_1.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.letschat.mvp_1.Service.GroupAddService;
import com.letschat.mvp_1.Service.GroupCreateService;

import reactor.core.publisher.Mono;

//@CrossOrigin(origins="*")
@CrossOrigin(originPatterns="*")
@RestController
@RequestMapping("/api/group")
public class GroupContoller {
    private final GroupCreateService groupCreateService;
    private final GroupAddService groupAddService;
    public GroupContoller(GroupCreateService groupCreateService,GroupAddService groupAddService){
        this.groupCreateService=groupCreateService;
        this.groupAddService=groupAddService;
    }

    @PostMapping("/create/{GroupName}")
    public Mono<ResponseEntity<String>> creategroup(@PathVariable String GroupName,@RequestHeader("User-Id") String UserId){
        return groupCreateService.creategroup(UserId, GroupName)
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed"))); 
    }

    @PostMapping("add/{GroupId}/{UserId}")
    public Mono<ResponseEntity<String>> addtogroup(@PathVariable String GroupId,@PathVariable String UserId){
        return groupAddService.addmember(GroupId, UserId)
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("error")));
    }
}
