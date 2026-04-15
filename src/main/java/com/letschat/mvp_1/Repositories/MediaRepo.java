package com.letschat.mvp_1.Repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.MediaInfo;

import reactor.core.publisher.Mono;

public interface MediaRepo extends ReactiveCrudRepository<MediaInfo,Long>{
    @Query("""
            select "media_id" from "MediaInfo" where "file_hash"=:hash limit 1
            """) 
            Mono<Long> findByHash(String hash);
}
