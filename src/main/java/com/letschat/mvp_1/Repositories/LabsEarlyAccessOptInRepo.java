package com.letschat.mvp_1.Repositories;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.LabsEarlyAccessOptIn;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LabsEarlyAccessOptInRepo extends ReactiveCrudRepository<LabsEarlyAccessOptIn, Long> {

    @Query("SELECT EXISTS(SELECT 1 FROM \"LabsEarlyAccessOptIn\" WHERE \"experiment_id\" = :experimentId AND \"user_id\" = :userId)")
    Mono<Boolean> hasUserOptedIn(Long experimentId, String userId);

    @Query("SELECT \"experiment_id\" FROM \"LabsEarlyAccessOptIn\" WHERE \"user_id\" = :userId")
    Flux<Long> findOptedInExperimentIds(String userId);

    @Query("SELECT COUNT(*) FROM \"LabsEarlyAccessOptIn\" WHERE \"experiment_id\" = :experimentId")
    Mono<Long> countByExperimentId(Long experimentId);

    @Modifying
    @Query("""
        INSERT INTO "LabsEarlyAccessOptIn" ("experiment_id", "user_id", "opted_in_at")
        VALUES (:experimentId, :userId, NOW())
        ON CONFLICT ("experiment_id", "user_id") DO NOTHING
    """)
    Mono<Integer> optIn(Long experimentId, String userId);
}
