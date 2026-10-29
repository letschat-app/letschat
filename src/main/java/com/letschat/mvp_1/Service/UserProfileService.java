package com.letschat.mvp_1.Service;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.Repositories.UserChatInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;

import reactor.core.publisher.Mono;

@Component
public class UserProfileService {
    private final UserInfoRepo userInfoRepo;
    private final ProfileService profileService;
    private final UserChatInfoRepo userChatInfoRepo;

    public UserProfileService (UserInfoRepo userInfoRepo, ProfileService profileService, UserChatInfoRepo userChatInfoRepo){
        this.userInfoRepo = userInfoRepo;
        this.profileService = profileService;
        this.userChatInfoRepo = userChatInfoRepo;
    }

    public Mono<String> upload(String mediaId, String UserId){
        return userInfoRepo.updateprofile(mediaId, UserId)
            .flatMap(info -> userChatInfoRepo.propagateProfilePicture(UserId, mediaId)
                .then(userChatInfoRepo.propagateProfilePictureToPeerChats(UserId, mediaId))
                .thenReturn("success " + info.getUserProfilePath()))
            .switchIfEmpty(Mono.just("failed"));
    }
}
