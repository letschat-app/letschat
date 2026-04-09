package com.letschat.mvp_1.Service;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.Repositories.UserInfoRepo;

import reactor.core.publisher.Mono;

@Component
public class UserProfileService {
    private final UserInfoRepo userInfoRepo;
    private final ProfileService profileService;
    public UserProfileService (UserInfoRepo userInfoRepo,ProfileService profileService){
        this.userInfoRepo=userInfoRepo;
        this.profileService=profileService;
    }

    public Mono<String> upload(String mediaId,String UserId){
        return userInfoRepo.updateprofile(mediaId, UserId)
        .flatMap(info->Mono.just("success "+info.getUserProfilePath()))
        .switchIfEmpty(Mono.just("failed"));
    }
}
