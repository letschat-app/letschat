package com.letschat.mvp_1.Service;

import org.springframework.stereotype.Service;
import com.letschat.mvp_1.Models.UserTokenModel;
import com.letschat.mvp_1.Repositories.FCMRepo;
import com.letschat.mvp_1.NotificationService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UserTokenService {

    private final FCMRepo fcmRepo;
    private final NotificationService notificationService;

    public UserTokenService(FCMRepo fcmRepo, NotificationService notificationService) {
        this.fcmRepo = fcmRepo;
        this.notificationService = notificationService;
    }

    // =========================
    // REGISTER TOKEN (FIXED)
    // =========================
    public Mono<UserTokenModel> registerToken(String userId, String deviceId, String token) {

        return fcmRepo.saveToken(userId, deviceId, token)
                .doOnNext(old -> {
                    notificationService.putToken(userId, token);
                });
    }

    // =========================
    // DELETE TOKEN
    // =========================
    public Mono<Void> removeToken(String deviceId) {
        return fcmRepo.deleteToken(deviceId);
    }

    // =========================
    // GET TOKENS
    // =========================
    public Flux<String> getUserTokens(String userId) {
        return fcmRepo.getTokens(userId);
    }
}