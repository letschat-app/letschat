package com.letschat.mvp_1.Repositories;

import java.time.LocalDateTime;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.DTOs.PulseViewerDTO;
import com.letschat.mvp_1.Models.PulseViewHistory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PulseViewHistoryRepo extends ReactiveCrudRepository<PulseViewHistory, Long> {

    @Query("""
            INSERT INTO "PulseViewHistory" ("pulse_id", "viewer_user_id", "viewed_at")
            VALUES (:pulseId, :viewerUserId, :viewedAt)
            ON CONFLICT DO NOTHING
            """)
    Mono<Void> recordView(Long pulseId, String viewerUserId, LocalDateTime viewedAt);

    @Query("""
            SELECT v."viewer_user_id" AS "viewerUserId", 
                   u."PublicName" AS "viewerName", 
                   u."UserProfilePath" AS "viewerProfile", 
                   v."viewed_at" AS "viewedAt"
            FROM "PulseViewHistory" v
            LEFT JOIN "UserInfo" u ON u."UserId" = v."viewer_user_id"
            WHERE v."pulse_id" = :pulseId
            ORDER BY v."viewed_at" DESC
            """)
    Flux<PulseViewerDTO> findViewersForPulse(Long pulseId);

    @Query("""
            SELECT COUNT(*) > 0 FROM "PulseViewHistory"
            WHERE "pulse_id" = :pulseId AND "viewer_user_id" = :viewerUserId
            """)
    Mono<Boolean> hasUserViewed(Long pulseId, String viewerUserId);

    @Query("""
            DELETE FROM "PulseViewHistory" WHERE "pulse_id" = :pulseId
            """)
    Mono<Void> deleteByPulseId(Long pulseId);
}
