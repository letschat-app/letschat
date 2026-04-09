package com.letschat.mvp_1.Repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.ScheduleMessage;

import reactor.core.publisher.Flux;

public interface ScheduleRepo extends ReactiveCrudRepository<ScheduleMessage,Long> {
    @Query("""
            select * from "ScheduleMessageInfo" where "time"<=NOW()
            """)
    Flux<ScheduleMessage> findMesages();

    
}
