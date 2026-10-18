package com.letschat.mvp_1.Repositories;

import java.time.LocalDateTime;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.SystemBroadcast;

import reactor.core.publisher.Flux;

public interface SystemBroadcastRepo extends ReactiveCrudRepository<SystemBroadcast, Long> {

    @Query("""
            SELECT * FROM "SystemBroadcast"
            WHERE "created_at" > :cutoff
            ORDER BY "created_at" ASC
            """)
    Flux<SystemBroadcast> findNewerThan(LocalDateTime cutoff);

    @Query("""
            SELECT * FROM "SystemBroadcast"
            ORDER BY "created_at" DESC
            LIMIT :limit
            """)
    Flux<SystemBroadcast> findLatest(int limit);
}
