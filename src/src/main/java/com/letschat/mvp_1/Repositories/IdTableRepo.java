package com.letschat.mvp_1.Repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.IdTableInfo;

import reactor.core.publisher.Mono;

public interface IdTableRepo extends ReactiveCrudRepository<IdTableInfo, Long> {
    @Query("""
            UPDATE "IdTable"
        SET "ChatId" = "ChatId" + 1
        RETURNING "ChatId"
            """)
        Mono<Long> getChatid();
    
    @Query("""
        UPDATE "IdTable"
        SET "UserId" = "UserId" + 1
        RETURNING "UserId"
    """)
    Mono<Long> getUserid();

    @Query("""
        UPDATE "IdTable"
        SET "GroupId" = "GroupId" + 1
        RETURNING "GroupId"
    """)
    Mono<Long> getGroupid();

    @Query("""
        UPDATE "IdTable"
        SET "ClassRoomId" = "ClassRoomId" + 1
        RETURNING "ClassRoomId"
    """)
    Mono<Long> getClassid();

}
