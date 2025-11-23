package com.letschat.mvp_1.Service;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.UserLoginDTO;
import com.letschat.mvp_1.Repositories.UserInfoRepo;

import reactor.core.publisher.Mono;
@Component
public class UserLoginService {

    private final UserInfoRepo userInfoRepo;
    public UserLoginService(UserInfoRepo userInfoRepo){
        this.userInfoRepo=userInfoRepo;
    }

    public Mono<String> logUser(UserLoginDTO request){
        System.out.println("logging"+request.getUserId());
        return userInfoRepo.findByUserId(request.getUserId())
        .flatMap(user->{
            if(user.getPassword().equals(request.getPassword())){
                return Mono.just("login succesfull");
            }
            else{
                return Mono.just("password incorrect");
            }
        })
        .switchIfEmpty(Mono.just("no user exist"));
    }
}
