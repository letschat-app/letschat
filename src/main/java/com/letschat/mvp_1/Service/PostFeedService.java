package com.letschat.mvp_1.Service;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.PostFeedDTO;
import com.letschat.mvp_1.Repositories.CommunityVideoRepo;

import reactor.core.publisher.Flux;

@Component
public class PostFeedService {
    private final CommunityVideoRepo communityVideoRepo;
    public PostFeedService(CommunityVideoRepo communityVideoRepo){
        this.communityVideoRepo=communityVideoRepo;
    }

    public Flux<PostFeedDTO> fetch(){
        return communityVideoRepo.fetch()
        .doOnNext(data->System.out.println(data.getCommunityName()+data.getVideoName()));
    }
}
