package com.letschat.mvp_1.Repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.ClassRoomInfo;

import reactor.core.publisher.Mono;

public interface ClassRoomRepo extends ReactiveCrudRepository<ClassRoomInfo, String>{
    @Query("""
            select * from "ClassRoomInfo" where "room_id"=:id
            """)
            Mono<ClassRoomInfo> findByRoomId(String id);

    @Query("""
            update "ClassRoomInfo" set "no_of_students"=:member where "room_id"=:id
            """)
            Mono<ClassRoomInfo> updateCount(String id,Long member);
}
