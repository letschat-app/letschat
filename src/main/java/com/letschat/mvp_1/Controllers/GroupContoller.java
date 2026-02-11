package com.letschat.mvp_1.Controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.letschat.mvp_1.DTOs.UserSearchResult;
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

    @PostMapping("/create/{GroupName0}")
    public Mono<ResponseEntity<String>> creategroup(@PathVariable String GroupName0,@RequestHeader("User-Id") String UserId,@RequestHeader("Group-Name") String GroupName){
        return groupCreateService.creategroup(UserId, GroupName)
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed"))); 
    }

    @PostMapping("add/{GroupId}/{UserId}")
    public Mono<ResponseEntity<String>> addtogroup(@PathVariable String GroupId,@PathVariable String UserId,@RequestHeader("User-Name") String Admin,@RequestHeader("User-Id") String AdminId){
        return groupAddService.addmember(GroupId, UserId, Admin,AdminId)
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("error")));
    }

    @GetMapping("/getmembers/{Chatid}")
    public Mono<List<UserSearchResult>> getMembers(@PathVariable String Chatid){
        return groupCreateService.getMembers(Chatid).collectList();
    }

    @PostMapping("promote/{chatid}/{UserId}")
    public Mono<ResponseEntity<String>> makeAdmin(@PathVariable String chatid,@PathVariable String UserId,@RequestHeader("User-Name") String Admin,@RequestHeader("User-Id") String AdminId){
        return groupAddService.makeAdmin(chatid, UserId, Admin,AdminId)
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("error")));
    }

    @PostMapping("remove/{Groupid}/{UserId}")
    public Mono<ResponseEntity<String>> removeMember(@PathVariable String Groupid,@PathVariable String UserId,@RequestHeader("User-Name") String Admin,@RequestHeader("User-Id") String AdminId){
        return groupAddService.removeMember(Groupid, UserId, Admin,AdminId)
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("error")));
    }


}
