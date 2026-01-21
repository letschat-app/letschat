package com.letschat.mvp_1.Repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.DTOs.PostFeedDTO;
import com.letschat.mvp_1.Models.CommunityVideoInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommunityVideoRepo extends ReactiveCrudRepository<CommunityVideoInfo,Long>{
    @Query("""
            insert into "CommunityVideoInfo"("VideoName","CommunityId","VideoPath","Description","Type")
            values(:vname,:cid,:vpath,:desc,:Type) returning *
            """)
            Mono<CommunityVideoInfo> insert(String vname,String cid,String vpath,String desc,String Type);

    @Query("""
            select cv."VideoName" ,cv."Description" as description,ci."CommunityName" as communityname,
cv."Likes" as likes,cv."DisLikes" as dislikes,cv."VideoPath" as videopath,cv."Type" as type from "CommunityVideoInfo" cv
join "CommunityInfo" ci on ci."CommunityId"=cv."CommunityId"
            """)
            Flux<PostFeedDTO> fetch();
}
