package com.letschat.mvp_1.Service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.Models.UserCommunityRelation;
import com.letschat.mvp_1.Repositories.UserCommunityRelationRepo;

import reactor.core.publisher.Mono;

@Component
public class UserCommunity {
    private final UserCommunityRelationRepo userCommunityRelationRepo;

    public UserCommunity(UserCommunityRelationRepo userCommunityRelationRepo){
        this.userCommunityRelationRepo=userCommunityRelationRepo;
    }

    public Mono<String> follow(String UserId,String CommunityId){
        UserCommunityRelation userCommunityRelation=new UserCommunityRelation();
        userCommunityRelation.setUserId(UserId);
        userCommunityRelation.setCommunityId(CommunityId);
        userCommunityRelation.setAt(LocalDateTime.now());
        userCommunityRelation.setRelation("following");
        return userCommunityRelationRepo.insert(UserId,CommunityId,LocalDateTime.now(),"following")
        .thenReturn("following");
    }

    public Mono<String> unfollow(String UserId,String CommunityId){
        return userCommunityRelationRepo.findByUserIdAndCommunityId(UserId,CommunityId)
        .flatMap(data->{
            System.out.println(data.getId()+data.getRelation()+data);
            return userCommunityRelationRepo.delete(data.getId())
            .thenReturn("unfollowed");
        })
        .switchIfEmpty(Mono.just("error"));
    }
}
