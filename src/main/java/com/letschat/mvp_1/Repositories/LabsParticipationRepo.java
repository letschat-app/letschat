package com.letschat.mvp_1.Repositories;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.LabsParticipation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LabsParticipationRepo extends ReactiveCrudRepository<LabsParticipation, Long> {

    @Query("SELECT * FROM \"LabsParticipation\" WHERE \"experiment_id\" = :experimentId AND \"user_id\" = :userId LIMIT 1")
    Mono<LabsParticipation> findByExperimentIdAndUserId(Long experimentId, String userId);

    @Query("SELECT * FROM \"LabsParticipation\" WHERE \"user_id\" = :userId")
    Flux<LabsParticipation> findAllByUserId(String userId);

    @Query("SELECT COUNT(*) FROM \"LabsParticipation\" WHERE \"experiment_id\" = :experimentId AND \"intent\" = :intent")
    Mono<Long> countByExperimentIdAndIntent(Long experimentId, String intent);

    @Modifying
    @Query("""
        INSERT INTO "LabsParticipation" ("experiment_id", "user_id", "intent", "voted_at")
        VALUES (:experimentId, :userId, :intent, NOW())
        ON CONFLICT ("experiment_id", "user_id")
        DO UPDATE SET "intent" = :intent, "voted_at" = NOW()
    """)
    Mono<Integer> upsertVote(Long experimentId, String userId, String intent);
}
