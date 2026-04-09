package com.letschat.mvp_1.Service;


import org.springframework.stereotype.Component;

import com.letschat.mvp_1.Models.MediaInfo;
import com.letschat.mvp_1.Repositories.MediaRepo;

import reactor.core.publisher.Mono;
@Component
public class MediaSaveService {
    private final String BaseUrl="https://pub-91d12b8ece574bde8ccd1c26b5367b6d.r2.dev/";
    private final MediaRepo mediaRepo;
    public MediaSaveService(MediaRepo mediaRepo){
        this.mediaRepo=mediaRepo;
    }

    public Mono<Long> insert(MediaInfo info){
        System.out.println(info.getFileKey());
        return mediaRepo.save(info)
        .flatMap(media->{
            System.out.println(media.getMediaId());
            return Mono.just(media.getMediaId());
    });
    }

    public Mono<MediaInfo> getMedia(Long id) {
        return mediaRepo.findById(id)
        .map(media -> {
            if (!media.getFileKey().startsWith("http")) {
                media.setFileKey(BaseUrl + media.getFileKey());
                media.setThumbnailKey(BaseUrl+media.getThumbnailKey());
            }
            return media;
        });
    }

}
