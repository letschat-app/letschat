package com.letschat.mvp_1.Service;

import java.time.LocalDateTime;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

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
            String defaultName = (user.getPrivateName() != null && !user.getPrivateName().trim().isEmpty())
                    ? user.getPrivateName().trim()
                    : user.getPublicName();
            return userChatInfoRepo.insert(chatid, user.getUserId(), Roomname, defaultName, now, now,"room","admin",null,user.getUserProfilePath())
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
        if (UsernameSimilarityEvaluator.isReserved(name)) {
            return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Nickname already taken"));
        }
        return userInfoRepo.setroomname(name,userid)
        .flatMap(user->Mono.just("success"));
    }
    public Mono<String> getRoomNickName(String userid){
        return userInfoRepo.getroomname(userid);
    }

    public static String normalizeRoomName(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase().replace('0', 'o').replaceAll("\\s+", " ");
    }

    public Mono<Map<String, Object>> checkRoomNameAvailability(String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) {
            return Mono.just(Map.of(
                "available", false,
                "normalized", "",
                "message", "Room name cannot be empty"
            ));
        }
        String normalized = normalizeRoomName(roomName);

        if (UsernameSimilarityEvaluator.isReserved(roomName)) {
            return Mono.just(Map.of(
                "available", false,
                "normalized", normalized,
                "message", "Room name already exists"
            ));
        }

        return roomInfoRepo.existsByNormalizedName(normalized)
            .map(exists -> {
                if (Boolean.TRUE.equals(exists)) {
                    return Map.<String, Object>of(
                        "available", false,
                        "normalized", normalized,
                        "message", "Room name already exists"
                    );
                } else {
                    return Map.<String, Object>of(
                        "available", true,
                        "normalized", normalized,
                        "message", "Room name is available"
                    );
                }
            });
    }

    public Mono<RoomInfo> createRoom(String userId, String roomName, String motto) {
        if (roomName == null || roomName.trim().isEmpty()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room name cannot be empty"));
        }
        String cleanRoomName = roomName.trim().replaceAll("\\s+", " ");
        if (UsernameSimilarityEvaluator.isReserved(roomName)) {
            return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Room name '" + cleanRoomName + "' already exists"));
        }
        String normalized = normalizeRoomName(roomName);

        return roomInfoRepo.existsByNormalizedName(normalized)
            .flatMap(exists -> {
                if (Boolean.TRUE.equals(exists)) {
                    return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Room name '" + cleanRoomName + "' already exists"));
                }
                return userInfoRepo.findByUserId(userId)
                    .flatMap(user -> generateChatId()
                        .flatMap(chatId -> {
                            LocalDateTime now = LocalDateTime.now();

                            String effectiveRoomNick = (user.getRoomNickname() != null && !user.getRoomNickname().trim().isEmpty())
                                    ? user.getRoomNickname().trim()
                                    : ((user.getPrivateName() != null && !user.getPrivateName().trim().isEmpty()) ? user.getPrivateName().trim() : user.getPublicName());

                            return userChatInfoRepo.insert(
                                    chatId,
                                    user.getUserId(),
                                    cleanRoomName,
                                    effectiveRoomNick,
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
                                            cleanRoomName,
                                            null,
                                            userId,
                                            chat.getId(),
                                            1L,
                                            motto
                                    )
                                    .flatMap(room ->
                                        myWebSocketHandler.announce(chatId,"created",cleanRoomName,user.getPrivateName(),userId)
                                        .thenReturn(room)
                                    )
                                )
                            );
                        })
                    );
            });
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
                    .switchIfEmpty(Mono.defer(() -> {
                        String effectiveRoomNick = (user.getRoomNickname() != null && !user.getRoomNickname().trim().isEmpty())
                                ? user.getRoomNickname().trim()
                                : ((user.getPrivateName() != null && !user.getPrivateName().trim().isEmpty()) ? user.getPrivateName().trim() : user.getPublicName());

                        return userChatInfoRepo.insertWithMute(
                            chat.getChatId(),
                            user.getUserId(),
                            room.getRoomName(),
                            effectiveRoomNick,
                            now,
                            now,
                            "room",
                            "viewer",
                            null,
                            user.getUserProfilePath(),
                            true
                        )
                        .then(Mono.fromRunnable(() ->
                            myWebSocketHandler.addUserToChatCache(chat.getChatId(), UserId)
                        ))
                        .then(roomInfoRepo.increaseMembers(RoomId).then())
                        .thenReturn(chat.getChatId());
                    }));
                });
            });
        })
        .switchIfEmpty(Mono.just("room not exist"));
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
                json.setIsMuted(chat.getIsMuted());
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
                json.setIsMuted(chat.getIsMuted());
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
