package com.letschat.mvp_1.Repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.DTOs.CommunityReturnDTO;
import com.letschat.mvp_1.Models.CommunityInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommunityInfoRepo extends ReactiveCrudRepository<CommunityInfo,String>{
    @Query("""
            insert into "CommunityInfo" ("CommunityId","CommunityName","Creator","ChatId","Motto","Type")
            values(:cid,:cname,:creator,:chatid,:motto,:type) returning *
            """)
        Mono<CommunityInfo> insert(String cid,String cname,String creator,Long chatid,String motto,String type);

        @Query("""
                select * from "CommunityInfo" order by "CommunityId" desc limit 1
                """)
    Mono<CommunityInfo> findlastuser();


    @Query("""
            select ci."CommunityId" as CommunityId,ci."CommunityName" as CommunityName,uc."ChatId" as ChatId,ci."Motto" as Motto,ci."NoOfMembers" as Followers from "CommunityInfo" ci
            join "UserChat" uc on ci."ChatId"=uc."Id"
where ci."Creator"=:userid
            """)
    Flux<CommunityReturnDTO> fetchmine(String userid);

    @Query("""
            select ci."CommunityId" as CommunityId,ci."CommunityName" as CommunityName,uc."ChatId" as ChatId,ci."Motto" as Motto,ci."NoOfMembers" as Followers from "CommunityInfo" ci
            join "UserChat" uc on ci."ChatId"=uc."Id"
where ci."Creator"!=:userid
            """)
    Flux<CommunityReturnDTO> fetchany(String userid);
}
