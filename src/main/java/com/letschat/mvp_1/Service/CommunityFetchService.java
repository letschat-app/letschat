package com.letschat.mvp_1.Service;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.CommunityReturnDTO;
import com.letschat.mvp_1.Repositories.CommunityInfoRepo;

import reactor.core.publisher.Flux;

@Component
public class CommunityFetchService {
    private final CommunityInfoRepo communityInfoRepo;
    public CommunityFetchService(CommunityInfoRepo communityInfoRepo){
        this.communityInfoRepo=communityInfoRepo;
    }

    public Flux<CommunityReturnDTO> fetchmine(String Userid){
        return communityInfoRepo.fetchmine(Userid)
        .doOnNext(data->System.out.println(data.getCommunityName()));
    }

    public Flux<CommunityReturnDTO> fetchany(String Userid){
        return communityInfoRepo.fetchany(Userid)
        .doOnNext(data->System.out.println(data.getCommunityName()));
    }
}
