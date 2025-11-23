package com.letschat.mvp_1.Repositories;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.EventInfo;

import reactor.core.publisher.Flux;


public interface EventInfoRepo extends ReactiveCrudRepository<EventInfo, Long>{
    Flux<EventInfo> findByUserId(String userId);
}
