package com.letschat.mvp_1.Repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.UserTokenModel;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FCMRepo extends ReactiveCrudRepository<UserTokenModel,String>{
    @Query("""
        INSERT INTO "UserFCMToken" (user_id, device_id, fcm_token)
        VALUES (:userId, :deviceId, :token)
        ON CONFLICT (user_id, device_id)
        DO UPDATE SET fcm_token = :token
        RETURNING *
    """)
    Mono<UserTokenModel> saveToken(String userId, String deviceId, String token);

    @Query("""
        SELECT fcm_token FROM "UserFCMToken"
        WHERE user_id = :userId
    """)
    Flux<String> getTokens(String userId);

    @Query("""
        DELETE FROM "UserFCMToken"
        WHERE device_id = :deviceId
        RETURNING user_id, device_id, fcm_token
    """)
    Mono<UserTokenModel> deleteToken(String deviceId);

    @Query("""
        DELETE FROM "UserFCMToken"
        WHERE fcm_token = :token
    """)
    Mono<Void> deleteByToken(String token);

    @Query("""
        SELECT * FROM "UserFCMToken"
        WHERE device_id = :deviceId
        LIMIT 1
    """)
    Mono<UserTokenModel> findByDeviceId(String deviceId);

    @Query("""
        SELECT user_id, fcm_token
        FROM "UserFCMToken"
    """)
    Flux<UserTokenModel> getAllUsersWithTokens();
}
