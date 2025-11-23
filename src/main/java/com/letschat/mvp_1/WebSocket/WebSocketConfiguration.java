package com.letschat.mvp_1.WebSocket;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

@Configuration
@EnableWebFlux
public class WebSocketConfiguration {
    @Bean
    public SimpleUrlHandlerMapping handlerMapping(WebSocketHandler handler){
        Map<String,WebSocketHandler> map=new HashMap<>();
        map.put("/chat",handler);
        SimpleUrlHandlerMapping mapping=new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1);
        return mapping;        
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter(){
        return new WebSocketHandlerAdapter();
    }
}
