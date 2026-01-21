package com.letschat.mvp_1.Repositories;

import java.time.LocalDateTime;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.UserCommunityRelation;

import reactor.core.publisher.Mono;


public interface UserCommunityRelationRepo extends ReactiveCrudRepository<UserCommunityRelation,Long>{
    @Query("""
            INSERT INTO "UserCommunityRelation" ("UserId", "CommunityId", "At", "Relation") VALUES (:uid,:cid,:at,:rel) returning *
            """)
    Mono<UserCommunityRelation> insert(String uid,String cid,LocalDateTime at,String rel);
    @Query("""
            select * from "UserCommunityRelation" where "UserId"=:userId and "CommunityId"=:communityId
            """)
    Mono<UserCommunityRelation>  findByUserIdAndCommunityId(String userId, String communityId);

    @Query("""
            delete from "UserCommunityRelation" where "Id"=:id
            """)
            Mono<UserCommunityRelation> delete(Long id);
}
