package com.letschat.mvp_1.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class MediaService {
    private final String rootpath="C:\\Users\\ADMIN\\Desktop\\letschat\\media\\uploads\\chats";

    public Mono<String> upload(FilePart file){
        //System.out.println("inner");
        String originalname=file.filename();
        System.out.println(originalname);
        String extention=originalname.split("\\.")[1];
        String filename="Media_"+UUID.randomUUID().toString()+"."+extention;
        Path path=Paths.get(rootpath,filename);
        return file.transferTo(path).thenReturn(filename);
    }

    public Mono<Resource> serve(String filename){
        try{
            System.out.println("serving");
            Path path=Paths.get(rootpath+"\\"+filename);
            Resource resource=new UrlResource(path.toUri());
            if(!resource.exists() || !resource.isReadable()){return Mono.empty();}
            return Mono.just(resource);
        }catch(IOException e){
            return Mono.empty();
        }
    }
    public Mono<String> gettype(String filename) throws IOException{
        Path path=Paths.get(rootpath+"\\"+filename);
        String contentType = Files.probeContentType(path);
        if (contentType == null) contentType = "application/octet-stream";
        return Mono.just(contentType);
    }
}
