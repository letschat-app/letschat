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
    // REGISTER TOKEN (FIXED - A3, A4)
    // =========================
    public Mono<UserTokenModel> registerToken(String userId, String deviceId, String token) {
        // Evict token from any previous user on this shared browser/device
        return fcmRepo.evictTokenFromOtherUsers(userId, token)
                .doOnNext(evicted -> {
                    if (evicted.getUserId() != null) {
                        notificationService.removeToken(evicted.getUserId(), token);
                        notificationService.invalidateTokenCache(evicted.getUserId());
                    }
                })
                .then(fcmRepo.saveToken(userId, deviceId, token))
                .doOnNext(saved -> {
                    notificationService.putToken(userId, token);
                    notificationService.invalidateTokenCache(userId);
                });
    }

    // =========================
    // DELETE TOKEN
    // =========================
    public Mono<Void> removeToken(String deviceId) {
        return fcmRepo.findByDeviceId(deviceId)
                .flatMap(tokenModel -> {
                    String uid = tokenModel.getUserId();
                    String tok = tokenModel.getFcmToken();
                    return fcmRepo.deleteToken(deviceId)
                            .doOnSuccess(ignored -> {
                                if (uid != null) {
                                    notificationService.removeToken(uid, tok);
                                    notificationService.invalidateTokenCache(uid);
                                }
                            });
                })
                .switchIfEmpty(Mono.defer(() -> fcmRepo.deleteToken(deviceId)))
                .then();
    }

    // =========================
    // GET TOKENS
    // =========================
    public Flux<String> getUserTokens(String userId) {
        return fcmRepo.getTokens(userId);
    }
}