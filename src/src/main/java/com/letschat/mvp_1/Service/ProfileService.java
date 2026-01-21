package com.letschat.mvp_1.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class ProfileService {
    private final String rootpath="C:\\Users\\ADMIN\\Desktop\\letschat\\media\\uploads\\profiles";

    public Mono<String> upload(FilePart file){
        //System.out.println("inner");
        String originalname=file.filename();
        String extention=originalname.split("\\.")[1];
        String filename="PROFILE_"+UUID.randomUUID().toString()+"."+extention;
        Path path=Paths.get(rootpath,filename);
        return file.transferTo(path).thenReturn(filename);
    }
}
