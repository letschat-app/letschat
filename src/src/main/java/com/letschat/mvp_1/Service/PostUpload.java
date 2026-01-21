package com.letschat.mvp_1.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.PostUploadDTO;
import com.letschat.mvp_1.Repositories.CommunityVideoRepo;

import reactor.core.publisher.Mono;

@Component
public class PostUpload {
    private final String rootpath="C:\\Users\\ADMIN\\Desktop\\letschat\\media\\uploads\\posts";

    private final CommunityVideoRepo communityVideoRepo;
    public PostUpload(CommunityVideoRepo communityVideoRepo){
        this.communityVideoRepo=communityVideoRepo;
    }

    public Mono<String> post(FilePart file,PostUploadDTO data){
        //return DataBufferUtils.join(null)
        System.out.println("filesize:"+file.filename());
        MediaType contenttype=file.headers().getContentType();
        System.out.println(contenttype.getType());
        String originalname=file.filename();
        String extention=originalname.split("\\.")[1];
        String filename="POST_"+UUID.randomUUID().toString()+"."+extention;
        Path path=Paths.get(rootpath,filename);        
        try {
            Files.createDirectories(Paths.get(rootpath));
            return file.transferTo(path)
            .doOnSuccess(aVoid -> {
                if (Files.exists(path)) {
                    System.out.println("File saved at: " + path.toAbsolutePath());
                } else {
                    System.out.println("File not saved!");
                }
            })
            .onErrorResume(e -> {
            e.printStackTrace();
            System.out.println("upload failed");
            return Mono.just(null);
            })
            .then(communityVideoRepo.insert(data.getVideoName(),data.getCommunityId(), filename, data.getDescription(),contenttype.getType()))
            .thenReturn("post uploaded");
        } catch (IOException e) {
            return Mono.just("upload failed");
        }
    }
}
