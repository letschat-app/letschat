package com.letschat.mvp_1.Service;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.UserLoginDTO;
import com.letschat.mvp_1.Repositories.UserInfoRepo;

import reactor.core.publisher.Mono;

@Component
public class UserLoginService {

    private final UserInfoRepo userInfoRepo;
    private final OtpService otpService;

    public UserLoginService(UserInfoRepo userInfoRepo, OtpService otpService){
        this.userInfoRepo = userInfoRepo;
        this.otpService = otpService;
    }

    public Mono<String> logUser(UserLoginDTO request){
        if (request == null || request.getUserId() == null || request.getPassword() == null) {
            return Mono.just("invalid request");
        }
        String identifier = request.getUserId().trim();
        System.out.println("logging: " + identifier);

        return userInfoRepo.findByIdentifier(identifier)
        .flatMap(user -> {
            if (user.getPassword().equals(request.getPassword())) {
                if (user.getIs2FAEnabled()) {
                    String targetEmail = user.getEmail() != null ? user.getEmail() : identifier;
                    return otpService.send2FAOtp(user.getUserId(), targetEmail)
                            .map(sent -> "2FA_REQUIRED:" + user.getUserId());
                } else {
                    return Mono.just("login succesfull");
                }
            } else {
                return Mono.just("password incorrect");
            }
        })
        .switchIfEmpty(Mono.just("no user exist"));
    }

    public Mono<Boolean> verify2FALogin(String identifier, String otp) {
        if (identifier == null || otp == null) {
            return Mono.just(false);
        }
        return userInfoRepo.findByIdentifier(identifier)
                .map(user -> otpService.verify2FAOtp(user.getUserId(), otp))
                .defaultIfEmpty(false);
    }
}
