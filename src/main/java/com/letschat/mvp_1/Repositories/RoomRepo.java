package com.letschat.mvp_1.Repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.RoomInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface RoomRepo extends ReactiveCrudRepository<RoomInfo,String>{

    @Query("""
        INSERT INTO "Room"
        (room_id, room_name, profile, creator, chat_id, no_of_members, motto)
        VALUES (:roomId, :roomName, :profile, :creator, :chatId, :noOfMembers, :motto)
        RETURNING *
    """)
    Mono<RoomInfo> insertRoom(
            String roomId,
            String roomName,
            String profile,
            String creator,
            Long chatId,
            Long noOfMembers,
            String motto
    );

    @Query("""
        UPDATE "Room"
        SET no_of_members = no_of_members + 1
        WHERE room_id = :roomId
    """)
    Mono<Integer> increaseMembers(String roomId);

    Flux<RoomInfo> findByCreator(String creator);
}
