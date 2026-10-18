package com.letschat.mvp_1.Repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.PulseInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PulseInfoRepo extends ReactiveCrudRepository<PulseInfo, Long> {

    @Query("""
            SELECT * FROM "PulseInfo" 
            WHERE "user_id" = :userId 
              AND "expires_at" > :cutoff 
            ORDER BY "created_at" DESC
            """)
    Flux<PulseInfo> findMyPulses(String userId, LocalDateTime cutoff);

    @Query("""
            SELECT * FROM "PulseInfo" 
            WHERE "status" = 'active' AND "expires_at" > :now 
              AND ("user_id" IN (:contactUserIds) OR "user_id" = 'SYSTEM_ADMIN' OR "user_id" = 'SYSTEM') 
              AND ("visibility" = 'chats_only' OR "visibility" = 'everyone' OR "visibility" IS NULL)
            ORDER BY "created_at" ASC
            """)
    Flux<PulseInfo> findActiveContactsPulses(List<String> contactUserIds, LocalDateTime now);

    @Query("""
            SELECT * FROM "PulseInfo" 
            WHERE "user_id" = :targetUserId AND "status" = 'active' AND "expires_at" > :now
              AND ("visibility" = 'everyone' OR ("visibility" = 'chats_only' AND :isContact = true))
            ORDER BY "created_at" ASC
            """)
    Flux<PulseInfo> findActiveProfilePulses(String targetUserId, boolean isContact, LocalDateTime now);

    @Query("""
            UPDATE "PulseInfo" 
            SET "status" = 'expired', "pulse_media_id" = NULL 
            WHERE "pulse_id" = :pulseId
            """)
    Mono<Void> markExpired(Long pulseId);

    @Query("""
            SELECT * FROM "PulseInfo" 
            WHERE ("status" = 'active' AND "expires_at" <= :now)
               OR ("status" = 'expired' AND "pulse_media_id" IS NOT NULL)
            """)
    Flux<PulseInfo> findExpiredPulsesWithMedia(LocalDateTime now);

    @Query("""
            SELECT * FROM "PulseInfo" 
            WHERE "status" = 'expired' AND "created_at" < :cutoff
            """)
    Flux<PulseInfo> findExpiredPulsesOlderThan(LocalDateTime cutoff);

    @Query("""
            SELECT COUNT(*) FROM "PulseInfo" 
            WHERE "status" = 'active' AND "expires_at" > :now
            """)
    Mono<Long> countActivePulses(LocalDateTime now);
}
