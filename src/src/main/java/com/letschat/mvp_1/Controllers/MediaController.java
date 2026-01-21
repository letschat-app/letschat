package com.letschat.mvp_1.Controllers;

//import java.net.http.HttpHeaders;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.letschat.mvp_1.DTOs.PostFeedDTO;
import com.letschat.mvp_1.DTOs.PostUploadDTO;
import com.letschat.mvp_1.Service.MediaService;
import com.letschat.mvp_1.Service.PostFeedService;
import com.letschat.mvp_1.Service.PostUpload;
import com.letschat.mvp_1.Service.ProfileService;
import com.letschat.mvp_1.Service.StreamService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

//@CrossOrigin(origins="*")
@CrossOrigin(originPatterns="*")
@RestController
@RequestMapping("/api/files")
public class MediaController {

    private final ProfileService profileService;
    private final PostUpload postUpload;
    private final PostFeedService postFeedService;
    private final StreamService streamService;
    private final MediaService mediaService;
    public MediaController(ProfileService profileService,PostUpload postUpload,PostFeedService postFeedService,StreamService streamService,MediaService mediaService){
        this.profileService=profileService;
        this.postUpload=postUpload;
        this.postFeedService=postFeedService;
        this.streamService=streamService;
        this.mediaService=mediaService;
    }
    @PostMapping("/uploads")
    public Mono<ResponseEntity<String>> uploads(@RequestPart("file") Mono<FilePart> fileMono){
        return fileMono.flatMap(profileService::upload)
        .map(filename->ResponseEntity.ok(filename))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed"))); 
    }

    @PostMapping("/media/upload")
    public Mono<ResponseEntity<String>> media_upload(@RequestPart("file") Mono<FilePart> fileMono){
        return fileMono.flatMap(mediaService::upload)
        .map(filename->ResponseEntity.ok(filename))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed"))); 
    }

    @GetMapping("/media/serve/{filename}")
    public Mono<ResponseEntity<Resource>> media_serve(@PathVariable String filename) throws IOException{
        System.out.println("called");
        return mediaService.serve(filename)
        .flatMap(resource->{           
            String contentType;
            Long len=(long)0;
            try {
                contentType = Files.probeContentType(resource.getFile().toPath());
                len=resource.contentLength();
            } catch (IOException ex) {
                contentType = "application/octet-stream";
            }
            return Mono.just(ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .contentLength(len)
            .header(HttpHeaders.CACHE_CONTROL, "public,max-age=3600")
            .header(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\""+resource.getFilename()+ "\"")
            .body(resource));
        })
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping(value="/upload/post",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<String>> uploadpost(@RequestPart("file") Mono<FilePart> fileMono,@RequestPart("metadata") Mono<String> requestMono){
        // return fileMono
        // .flatMap(file->{
        //     return postUpload.post(file, request)
        //     .map(data->ResponseEntity.ok(data))
        //     .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed")));
        // })
        // .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed")));
        System.out.println("inside");
        System.out.println(requestMono);
        return Mono.zip(fileMono, requestMono)
        .flatMap(tuple -> {
            FilePart file = tuple.getT1();
            String requeststr=tuple.getT2();
            ObjectMapper obj=new ObjectMapper();
            PostUploadDTO request;
            try {
                request=obj.readValue(requeststr,PostUploadDTO.class);
            } catch (JsonProcessingException e) {
                System.out.println("wrong format");
                return Mono.just(ResponseEntity.status(400).body("wrong format"));
            }
           // PostUploadDTO request =null;//= tuple.getT2();

            return postUpload.post(file, request)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed")));
        })
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed")));
    }

    @GetMapping("/fetch/feed")
    public Mono<List<PostFeedDTO>> feed(@RequestHeader("User-Id") String UserId){
        System.out.println("feed");
        return postFeedService.fetch().collectList();
    }

    @GetMapping(value="/stream/{filename}",produces="video/mp4")
    public Flux<DataBuffer> stream(@PathVariable String filename,@RequestHeader HttpHeaders headers){
        System.out.println("straming");
        String range=headers.getFirst(HttpHeaders.RANGE);
        
        return streamService.stream(filename,range);
    }
}
