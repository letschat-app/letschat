package com.letschat.mvp_1.Controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.letschat.mvp_1.DTOs.CommunityCreationDTO;
import com.letschat.mvp_1.DTOs.CommunityReturnDTO;
import com.letschat.mvp_1.Service.CommunityCreationService;
import com.letschat.mvp_1.Service.CommunityFetchService;
import com.letschat.mvp_1.Service.UserCommunity;

import reactor.core.publisher.Mono;

@CrossOrigin(origins="*")
@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityCreationService communityCreationService;
    private final CommunityFetchService communityFetchService;
    private final UserCommunity userCommunity;
    public CommunityController(CommunityCreationService communityCreationService,CommunityFetchService communityFetchService,UserCommunity userCommunity){
        this.communityCreationService=communityCreationService;
        this.communityFetchService=communityFetchService;
        this.userCommunity=userCommunity;
    }

    @PostMapping("/create")
    public Mono<ResponseEntity<String>> createcommunity(@RequestBody CommunityCreationDTO request,@RequestHeader("User-Id") String UserId){
        System.out.println("community craete");
        return communityCreationService.create(request,UserId)
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed"))); 
    }

    @GetMapping("/fetch/mycommunity")
    public Mono<List<CommunityReturnDTO>> fetchmy(@RequestHeader("User-Id") String UserId){
        System.out.println("fetching mine");
        return communityFetchService.fetchmine(UserId).collectList();
    }

    @GetMapping("/fetch/any")
    public Mono<List<CommunityReturnDTO>> fetchany(@RequestHeader("User-Id") String UserId){
        System.out.println("fetching mine");
        return communityFetchService.fetchany(UserId).collectList();
    }

    @PostMapping("/follow/{cid}")
    public Mono<ResponseEntity<String>> follow(@PathVariable String cid,@RequestHeader("User-Id") String UserId){
        System.out.println("follow");
        return userCommunity.follow(UserId, cid)
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("error")));
    }

    @PostMapping("/unfollow/{cid}")
    public Mono<ResponseEntity<String>> unfollow(@PathVariable String cid,@RequestHeader("User-Id") String UserId){
        System.out.println("follow");
        return userCommunity.unfollow(UserId, cid)
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("error")));
    }
}
