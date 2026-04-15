package com.letschat.mvp_1.Service;


import org.springframework.stereotype.Service;

import com.letschat.mvp_1.Models.UserTokenModel;
import com.letschat.mvp_1.Repositories.FCMRepo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UserTokenService {

    private FCMRepo fcmRepo;
    public UserTokenService(FCMRepo fcmRepo){
        this.fcmRepo=fcmRepo;
    }


    // insert / update token
    public Mono<UserTokenModel> registerToken(String userId,String deviceId,String token){
        return fcmRepo.saveToken(userId, deviceId, token);
    }


    // delete token (disable notification)
    public Mono<Void> removeToken(String deviceId){
        return fcmRepo.deleteToken(deviceId);
    }


    // retrieve tokens (for sending notifications)
    public Flux<String> getUserTokens(String userId){
        return fcmRepo.getTokens(userId);
    }

}

