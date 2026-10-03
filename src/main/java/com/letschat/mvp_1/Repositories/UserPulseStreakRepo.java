package com.letschat.mvp_1.Repositories;

import java.time.LocalDate;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.UserPulseStreak;

import reactor.core.publisher.Mono;

public interface UserPulseStreakRepo extends ReactiveCrudRepository<UserPulseStreak, String> {

    @Query("""
            INSERT INTO "UserPulseStreak" ("user_id", "current_streak", "last_pulse_date")
            VALUES (:userId, :streak, :lastDate)
            ON CONFLICT ("user_id") 
            DO UPDATE SET "current_streak" = EXCLUDED."current_streak", "last_pulse_date" = EXCLUDED."last_pulse_date"
            RETURNING *
            """)
    Mono<UserPulseStreak> upsertStreak(String userId, Integer streak, LocalDate lastDate);
}
