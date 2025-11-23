package com.letschat.mvp_1.Service;


import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.UserSearchResult;
import com.letschat.mvp_1.Repositories.UserInfoRepo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class UserSearchService {

    private final UserInfoRepo userInfoRepo;
    public UserSearchService(UserInfoRepo userInfoRepo){
        this.userInfoRepo=userInfoRepo;
    }
    public Mono<UserSearchResult> search(String UserId){
        System.out.println("searching:"+UserId);
        return userInfoRepo.findByUserId(UserId)
        .flatMap(user->{
            UserSearchResult User=new UserSearchResult();
            User.setUserId(user.getUserId());
            User.setUserName(user.getPublicName());
            return Mono.just(User);
        });
    }
    public Flux<UserSearchResult> explore(){
        System.out.println("explore");
        return userInfoRepo.findAll()
        .flatMap(user->{
            UserSearchResult User=new UserSearchResult();
            User.setUserId(user.getUserId());
            User.setUserName(user.getPublicName());
            return Mono.just(User);
        });
    }
}
