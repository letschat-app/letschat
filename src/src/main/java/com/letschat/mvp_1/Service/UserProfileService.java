package com.letschat.mvp_1.Service;

import org.springframework.http.codec.multipart.FilePart;
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

    public Mono<String> upload(FilePart file,String UserId){
       // System.out.println("outer");
        return profileService.upload(file)
        .flatMap(filename->{
          //  System.out.println(filename+","+UserId);
            return userInfoRepo.updateprofile(filename, UserId)
            .thenReturn(filename);
        })
        .switchIfEmpty(Mono.just("failed"));
    }
}
