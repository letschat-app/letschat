package com.letschat.mvp_1.Repositories;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.MediaInfo;

public interface MediaRepo extends ReactiveCrudRepository<MediaInfo,Long>{
    
}
