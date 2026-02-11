package com.letschat.mvp_1.Service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.Repositories.ClassRoomRepo;
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
    private final ClassRoomRepo classRoomRepo;
    public GroupAddService(UserInfoRepo userInfoRepo,UserChatInfoRepo userChatInfoRepo,GroupInfoRepo groupInfoRepo,MyWebSocketHandler myWebSocketHandler,ClassRoomRepo classRoomRepo){
        this.userInfoRepo=userInfoRepo;
        this.userChatInfoRepo=userChatInfoRepo;
        this.groupInfoRepo=groupInfoRepo;
        this.myWebSocketHandler=myWebSocketHandler;
        this.classRoomRepo=classRoomRepo;
    }

    public Mono<String> addmember(String GroupId,String UserId,String AdminName,String AdminId){
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
                        "group",
                        "member"
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
                    myWebSocketHandler.announce(chat.getChatId(), "added", user.getPublicName(),AdminName,AdminId)
                )
                .then(groupInfoRepo.updatemembers(GroupId, group.getNoOfMembers() + 1))
                .thenReturn("added");
            });
        });
    });

    }

    public Mono<String> joinclass(String RoomId,String UserId){
        LocalDateTime now=LocalDateTime.now();
        return classRoomRepo.findByRoomId(RoomId)
        .flatMap(room->{
            return userInfoRepo.findByUserId(UserId)
            .flatMap(user->{
                return userChatInfoRepo.findByChatId(room.getChatId())

                .flatMap(chat->{
                    return userChatInfoRepo.joinedOrNot(chat.getChatId(), UserId)
                    .flatMap(chatid->Mono.just(chatid))
                    .switchIfEmpty(
                     userChatInfoRepo.insert(
                        chat.getChatId(),
                        user.getUserId(),
                        room.getRoomName(),
                        user.getPublicName(),
                        now,
                        now,
                        "classroom",
                        "student"
                    )
                
                .then(classRoomRepo.updateCount(RoomId, room.getNoOfStudents() + 1).then())
                .thenReturn(chat.getChatId()));
                });
            });
        })
        .switchIfEmpty(Mono.just("classroom not exist"));
    }


    public Mono<String> makeAdmin(String chatid,String userid,String adminName,String adminid){
        return groupInfoRepo.makeAdmin(chatid, userid)
        .flatMap(chat->{
            return userChatInfoRepo.getInfo(chatid, userid)
            .flatMap(user->{
                System.out.println("user "+user.getType());
                 return myWebSocketHandler.announce(chatid, "promote", user.getUserName(), adminName,adminid)
            .thenReturn("ok");
            });
        })
        .switchIfEmpty(Mono.just("error"));
    }
    
    public Mono<String> removeMember(String GroupId,String UserId,String AdminName,String adminid){
        return groupInfoRepo.findByGroupId(GroupId)
        .flatMap(group->{
            return userChatInfoRepo.findByChatId(group.getChatId())
            .flatMap(chat->{
                return userChatInfoRepo.getUserChat(chat.getChatId(), UserId)
                .flatMap(userchat->{
                    return myWebSocketHandler.announce(chat.getChatId(), "removed", userchat.getUserName(), AdminName,adminid)
                    .then(userChatInfoRepo.updateStatus("removed",userchat.getChatId(),UserId))
                    .then(groupInfoRepo.updatemembers(GroupId, group.getNoOfMembers() - 1))
                    .thenReturn("ok");
                });
            });
            
            
        });
    }
    
}
