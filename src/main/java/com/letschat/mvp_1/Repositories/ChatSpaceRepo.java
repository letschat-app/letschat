package com.letschat.mvp_1.Repositories;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.ChatSpace;

import reactor.core.publisher.Flux;



public interface ChatSpaceRepo extends ReactiveCrudRepository<ChatSpace,Long>{

    Flux<ChatSpace> findByChatId(String chatId);
}
