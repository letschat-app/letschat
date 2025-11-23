package com.letschat.mvp_1.Service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.Repositories.GroupInfoRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;
import com.letschat.mvp_1.WebSocket.MyWebSocketHandler;

import reactor.core.publisher.Mono;

@Component
public class GroupAddService {
    private final UserInfoRepo userInfoRepo;
    private final UserChatInfoRepo userChatInfoRepo;
    private final GroupInfoRepo groupInfoRepo;
    private final MyWebSocketHandler myWebSocketHandler;
    public GroupAddService(UserInfoRepo userInfoRepo,UserChatInfoRepo userChatInfoRepo,GroupInfoRepo groupInfoRepo,MyWebSocketHandler myWebSocketHandler){
        this.userInfoRepo=userInfoRepo;
        this.userChatInfoRepo=userChatInfoRepo;
        this.groupInfoRepo=groupInfoRepo;
        this.myWebSocketHandler=myWebSocketHandler;
    }

    public Mono<String> addmember(String GroupId,String UserId){
        System.out.println("adding");
        LocalDateTime now=LocalDateTime.now();
        // return groupInfoRepo.findByGroupId(GroupId)
        // .flatMap(group->userInfoRepo.findByUserId(UserId)
        // .flatMap(user->userChatInfoRepo.findById(group.getChatId())
        // .flatMap(chat->userChatInfoRepo.insert(chat.getChatId(),UserId,group.getGroupName(),user.getPublicName(),now,now).thenReturn("added"));););
        // .switchIfEmpty(Mono.just("failed"));

       return groupInfoRepo.findByGroupId(GroupId)
    .flatMap(group -> {
        return userInfoRepo.findByUserId(UserId)
        .flatMap(user -> {
            return userChatInfoRepo.findByChatId(group.getChatId())
            .flatMap(chat -> {
                return userChatInfoRepo.insert(
                        chat.getChatId(),
                        user.getUserId(),
                        group.getGroupName(),
                        user.getPublicName(),
                        now,
                        now,
                        "group"
                    )
                .then(
                    // userChatInfoRepo.insert(
                    //     chat.getChatId(),
                    //     user.getUserId(),
                    //     group.getGroupName(),
                    //     user.getPublicName(),
                    //     now,
                    //     now,
                    //     "group"
                    // )
                    myWebSocketHandler.announce(chat.getChatId(), "added", user.getPublicName())
                )
                .then(groupInfoRepo.updatemembers(GroupId, group.getNoOfMembers() + 1))
                .thenReturn("added");
            });
        });
    });

    }
}
