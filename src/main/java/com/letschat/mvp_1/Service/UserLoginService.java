package com.letschat.mvp_1.Service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.UserLoginDTO;
import com.letschat.mvp_1.Models.UserInfo;
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

    public Mono<Map<String, Object>> logUser(UserLoginDTO request){
        if (request == null || request.getUserId() == null || request.getPassword() == null) {
            return Mono.just(Map.<String, Object>of("status", "ERROR", "message", "invalid request"));
        }
        String identifier = request.getUserId().trim();
        System.out.println("logging: " + identifier);

        return userInfoRepo.findByIdentifier(identifier)
        .flatMap(user -> {
            if (user.getPassword().equals(request.getPassword())) {
                if (Boolean.TRUE.equals(user.getIs2FAEnabled())) {
                    String targetEmail = user.getEmail() != null ? user.getEmail() : identifier;
                    return otpService.send2FAOtp(user.getUserId(), targetEmail)
                            .map(sent -> Map.<String, Object>of(
                                "status", "2FA_REQUIRED",
                                "userId", user.getUserId(),
                                "message", "2-Factor Authentication OTP sent to your registered email"
                            ));
                } else {
                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "SUCCESS");
                    data.put("message", "login succesfull");
                    data.put("userId", user.getUserId());
                    data.put("username", user.getPublicName());
                    data.put("displayName", user.getPrivateName() != null && !user.getPrivateName().trim().isEmpty() ? user.getPrivateName() : user.getPublicName());
                    data.put("email", user.getEmail() != null ? user.getEmail() : "");
                    data.put("profile", user.getUserProfilePath() != null ? user.getUserProfilePath() : "");
                    data.put("is2FAEnabled", false);
                    return Mono.just(data);
                }
            } else {
                return Mono.just(Map.<String, Object>of("status", "ERROR", "message", "password incorrect"));
            }
        })
        .switchIfEmpty(Mono.just(Map.<String, Object>of("status", "ERROR", "message", "no user exist")));
    }

    public Mono<UserInfo> verify2FALogin(String identifier, String otp) {
        if (identifier == null || otp == null) {
            return Mono.empty();
        }
        return userInfoRepo.findByIdentifier(identifier)
                .filter(user -> otpService.verify2FAOtp(user.getUserId(), otp));
    }
}
