package com.letschat.mvp_1.Service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.ChatBoxReturnDTO;
import com.letschat.mvp_1.Models.RoomInfo;
import com.letschat.mvp_1.Repositories.IdTableRepo;
import com.letschat.mvp_1.Repositories.RoomRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;
import com.letschat.mvp_1.WebSocket.MyWebSocketHandler;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class RoomService {
    private final UserInfoRepo userInfoRepo;
    private final UserChatInfoRepo userChatInfoRepo;
    private final RoomRepo roomInfoRepo;
    private final IdTableRepo idTableRepo;
    private final MyWebSocketHandler myWebSocketHandler;
    public RoomService(IdTableRepo idTableRepo,UserInfoRepo userInfoRepo,UserChatInfoRepo userChatInfoRepo,RoomRepo roomInfoRepo,MyWebSocketHandler myWebSocketHandler){
        this.userInfoRepo=userInfoRepo;
        this.userChatInfoRepo=userChatInfoRepo;
        this.roomInfoRepo=roomInfoRepo;
        this.idTableRepo=idTableRepo;
        this.myWebSocketHandler=myWebSocketHandler;
    }  


    public Mono<String> createRoom0(String UserId,String Roomname,String motto){
        return userInfoRepo.findById(UserId)
        .flatMap(user->generateChatId()
        .flatMap(chatid->{
            LocalDateTime now=LocalDateTime.now();
            return userChatInfoRepo.insert(chatid, user.getUserId(), Roomname, user.getPrivateName(), now, now,"room","admin",null,user.getUserProfilePath())
            .flatMap(chat->{
                return generateGroupId()
                .flatMap(roomid->{
                    return roomInfoRepo.insertRoom(roomid, Roomname, null, UserId, chat.getId(), 1L, motto)
                    .thenReturn("created");
                });
            });
        }))
        .switchIfEmpty(Mono.just("failed"));
    }

    public Mono<String> createRoomNickname(String name,String userid){
        return userInfoRepo.setroomname(name,userid)
        .flatMap(user->Mono.just("success"));
    }
    public Mono<String> getRoomNickName(String userid){
        return userInfoRepo.getroomname(userid);
    }

    public Mono<RoomInfo> createRoom(String userId, String roomName, String motto) {

    return userInfoRepo.findByUserId(userId)
        .flatMap(user -> generateChatId()
            .flatMap(chatId -> {

                LocalDateTime now = LocalDateTime.now();

                return userChatInfoRepo.insert(
                        chatId,
                        user.getUserId(),
                        roomName,
                        user.getRoomNickname(),
                        now,
                        now,
                        "room",
                        "admin",
                        null,
                        user.getUserProfilePath()
                )
                .flatMap(chat -> generateGroupId()
                    .flatMap(roomId ->
                        roomInfoRepo.insertRoom(
                                roomId,
                                roomName,
                                null,
                                userId,
                                chat.getId(),
                                1L,
                                motto
                        )
                        .flatMap(room ->
                            myWebSocketHandler.announce(chatId,"created",roomName,user.getPrivateName(),userId)
                            .thenReturn(room)
                        )
                    )
                );
            })
        );
    }

    public Mono<String> joinroom(String RoomId,String UserId){
        LocalDateTime now=LocalDateTime.now();
        return roomInfoRepo.findById(RoomId)
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
                        user.getRoomNickname(),
                        now,
                        now,
                        "room",
                        "viewer",
                        null,
                        user.getUserProfilePath()
                    )
                .then(Mono.fromRunnable(() ->
                    myWebSocketHandler.addUserToChatCache(chat.getChatId(), UserId)
                ))
                .then(roomInfoRepo.increaseMembers(RoomId).then())
                .thenReturn(chat.getChatId()));
                });
            });
        })
        .switchIfEmpty(Mono.just("classroom not exist"));
    }

    public Mono<RoomInfo> view(String roomid){
        return roomInfoRepo.findById(roomid);
    }

    public Flux<ChatBoxReturnDTO> load(){
        return roomInfoRepo.findAll()
        .flatMap(room->{
            return userChatInfoRepo.findByChatId(room.getChatId())
            .flatMap(chat->{
                ChatBoxReturnDTO json=new ChatBoxReturnDTO();
                json.setChatName(room.getRoomName());
                json.setChatId(chat.getChatId());
                json.setId(room.getRoomId());
                json.setProfile(room.getProfile());
                json.setNoOfMembers(room.getNoOfMembers());
                json.setStatus(room.getMotto());
                return Mono.just(json);
            });
        });
    }

    public Flux<ChatBoxReturnDTO> load(String userId){
        return roomInfoRepo.findByCreator(userId)
        .flatMap(room->{
            return userChatInfoRepo.findByChatId(room.getChatId())
            .flatMap(chat->{
                ChatBoxReturnDTO json=new ChatBoxReturnDTO();
                json.setChatName(room.getRoomName());
                json.setChatId(chat.getChatId());
                json.setId(room.getRoomId());
                json.setProfile(room.getProfile());
                return Mono.just(json);
            });
        });
    }

    public Mono<String> generateChatId(){
        return idTableRepo.getChatid()
        .flatMap(id->{
            return numberToCode(id);
        });
    }
    public Mono<String> generateGroupId(){
        return idTableRepo.getRoomid()
        .flatMap(id->{
            return numberToCode(id);
        });
    }

    private Mono<String> numberToCode(Long num) {
        return Mono.fromSupplier(() -> {
            int lettersIndex = (int) (num / 1000);
            int digits = (int) (num % 1000);

            char first = (char) ('A' + (lettersIndex / (26 * 26)) % 26);
            char second = (char) ('A' + (lettersIndex / 26) % 26);
            char third = (char) ('A' + lettersIndex % 26);

            return String.format("%c%c%c%03d", first, second, third, digits);
        });
    }

}
