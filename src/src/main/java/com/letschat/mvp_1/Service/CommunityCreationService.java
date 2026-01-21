package com.letschat.mvp_1.Service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.CommunityCreationDTO;
import com.letschat.mvp_1.Repositories.CommunityInfoRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;

import reactor.core.publisher.Mono;

@Component
public class CommunityCreationService {
    private final CommunityInfoRepo communityInfoRepo;
    private final UserChatInfoRepo userChatInfoRepo;
    public CommunityCreationService(CommunityInfoRepo communityInfoRepo,UserChatInfoRepo userChatInfoRepo){
        this.communityInfoRepo=communityInfoRepo;
        this.userChatInfoRepo=userChatInfoRepo;
    }

    public Mono<String> create(CommunityCreationDTO request,String Userid){
        return generateChatId()
        .flatMap(chid->{
            return userChatInfoRepo.insert(chid,Userid ,request.getCommunityName(), Userid, LocalDateTime.now(), LocalDateTime.now(), "community","Owner")
            .flatMap(chat->{
                return generateId()
                .flatMap(cid->{
                    return communityInfoRepo.insert(cid, request.getCommunityName(), Userid, chat.getId(), request.getMotto(), request.getType())
                    .thenReturn("created:"+cid);
                });
            });
        });
    }

    public Mono<String> generateChatId(){
        return userChatInfoRepo.findlastuser()
        .doOnNext(u->System.out.println(u))
        .map(chat->{
           // System.out.println("in generate:"+user.getUserId()+user.getAge());
            String id=chat.getChatId();//AAA000
            System.out.println(id);
            int num =Integer.parseInt(id.replaceAll("[^0-9]", ""));
            return String.format("AAA%03d", num+1);
        })
        .defaultIfEmpty("AAA000");
    }
    public Mono<String> generateId(){
        return communityInfoRepo.findlastuser()
        .doOnNext(u->System.out.println(u))
        .map(chat->{
           // System.out.println("in generate:"+user.getUserId()+user.getAge());
            String id=chat.getCommunityId();//AAA000
            System.out.println(id);
            int num =Integer.parseInt(id.replaceAll("[^0-9]", ""));
            return String.format("AAA%03d", num+1);
        })
        .defaultIfEmpty("AAA000");
    }
}
