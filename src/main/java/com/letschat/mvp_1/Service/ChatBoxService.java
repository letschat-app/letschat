package com.letschat.mvp_1.Service;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.ChatBoxReturnDTO;
import com.letschat.mvp_1.Repositories.GroupInfoRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;

import reactor.core.publisher.Flux;

@Component
public class ChatBoxService {
    private final UserChatInfoRepo userChatInfoRepo;
    public ChatBoxService(UserInfoRepo userInfoRepo,UserChatInfoRepo userChatInfoRepo,GroupInfoRepo groupInfoRepo){
        this.userChatInfoRepo=userChatInfoRepo;
    }

    public Flux<ChatBoxReturnDTO> getchat(String UserId){
        System.out.println("getting");
        // return userChatInfoRepo.findUserChat(UserId)
        // .map(user-> user.getPublicName())
        // .switchIfEmpty(
        //     userChatInfoRepo.findGroupChat(UserId)
        //     .map(group->group.getGroupName())
        // );

        Flux<ChatBoxReturnDTO> username=userChatInfoRepo.findUserChat(UserId)
        .map(user->{
           // System.out.println(user.getChatId()+","+user.getChatName()+","+user.getId()+","+user.getType());
           //System.out.println(user.gettimestamp()) ;
           return user;
        });
        Flux<ChatBoxReturnDTO> groupname=userChatInfoRepo.findGroupChat(UserId)
        .map(group->{
           // System.out.println(group.getChatId()+","+group.getChatName()+","+group.getId()+","+group.getType());
            return group;
        });
        Flux<ChatBoxReturnDTO> roomname=userChatInfoRepo.findClassRoomChat(UserId)
        .map(room->{
            return room;
        });
        return Flux.concat(username,groupname,roomname);
    }
}
