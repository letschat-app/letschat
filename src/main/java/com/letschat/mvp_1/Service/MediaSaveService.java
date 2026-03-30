package com.letschat.mvp_1.Service;


import com.letschat.mvp_1.Models.MediaInfo;
import com.letschat.mvp_1.Repositories.MediaRepo;

import reactor.core.publisher.Mono;

public class MediaSaveService {
    private final MediaRepo mediaRepo;
    public MediaSaveService(MediaRepo mediaRepo){
        this.mediaRepo=mediaRepo;
    }

    public Mono<Long> insert(MediaInfo info){
        return mediaRepo.save(info)
        .flatMap(media->Mono.just(media.getMediaId()));
    }

    public Mono<MediaInfo> getMedia(Long id){
        return mediaRepo.findById(id)
        .flatMap(media->Mono.just(media));
    }
}
