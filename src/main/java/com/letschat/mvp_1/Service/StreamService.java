package com.letschat.mvp_1.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;

@Component
public class StreamService {
    private final String rootpath="C:\\Users\\ADMIN\\Desktop\\letschat\\media\\uploads\\posts";
    public Flux<DataBuffer> stream(String filename,String ranges){
        try {
            Path path=Paths.get(rootpath,filename);
            if (!Files.exists(path)) {
                return Flux.error(new RuntimeException("video not found"));
            }
            DefaultDataBufferFactory factory=new DefaultDataBufferFactory();
            return DataBufferUtils.read(path,factory,409600);
        } catch (Exception e) {
            return Flux.error(e);
        }
    }
}
