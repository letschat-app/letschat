package com.letschat.mvp_1.Repositories;

import java.time.Instant;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.EventInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface EventInfoRepo extends ReactiveCrudRepository<EventInfo, Long>{
    Flux<EventInfo> findByUserId(String userId);

    @Query("""
            update "EventInfo" set action=:action, event_color=:color where event_id=:id
            """)
        Mono<EventInfo> updateaction(Long id,String action,String color);

    @Query("""
    SELECT *
    FROM "EventInfo"
    WHERE start_time BETWEEN :start AND :end
    AND is_sync = true
    """)
    Flux<EventInfo> findEventsBetween(Instant start, Instant end);

    @Query("""
    SELECT *
    FROM "EventInfo"
    WHERE start_time BETWEEN :startOfDay AND :endOfDay
    AND is_sync = true
    """)
    Flux<EventInfo> findTodayEvents(Instant startOfDay, Instant endOfDay);


}
