package com.letschat.mvp_1.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.letschat.mvp_1.DTOs.UserSearchResult;
import com.letschat.mvp_1.Models.ChatSpace;
import com.letschat.mvp_1.Models.UserChatInfo;
import com.letschat.mvp_1.Models.UserInfo;
import com.letschat.mvp_1.Repositories.ChatSpaceRepo;
import com.letschat.mvp_1.Repositories.IdTableRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;
import com.letschat.mvp_1.WebSocket.MyWebSocketHandler;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.letschat.mvp_1.Repositories.UserPulseStreakRepo;

@Component
public class UserChatService {
    private final UserInfoRepo userInfoRepo;
    private final UserChatInfoRepo userChatInfoRepo;
    private final IdTableRepo idTableRepo;
    private final MyWebSocketHandler myWebSocketHandler;
    private final ChatSpaceRepo chatSpaceRepo;
    private final UserPulseStreakRepo userPulseStreakRepo;

    public UserChatService(UserInfoRepo userInfoRepo,UserChatInfoRepo userChatInfoRepo,IdTableRepo idTableRepo,MyWebSocketHandler myWebSocketHandler,ChatSpaceRepo chatSpaceRepo, UserPulseStreakRepo userPulseStreakRepo){
        this.userInfoRepo=userInfoRepo;
        this.userChatInfoRepo=userChatInfoRepo;
        this.idTableRepo=idTableRepo;
        this.myWebSocketHandler=myWebSocketHandler;
        this.chatSpaceRepo=chatSpaceRepo;
        this.userPulseStreakRepo=userPulseStreakRepo;
    }

    // public Mono<String> addchat1(String SenderId,String RecieverId){
    //     return userInfoRepo.findByUserId(SenderId)
    //     .flatMap(senduser->()->generateId().flatMap(chatid->{
    //         return userInfoRepo.findByUserId(RecieverId)
    //         .flatMap(receiveuser->{
    //             LocalDateTime now=LocalDateTime.now();
    //             UserChatInfo sendchat=new UserChatInfo();
    //             sendchat.setChatId(chatid);
    //             sendchat.setUserId(SenderId);
                // sendchat.setChatName(receiveuser.getPublicName());
                // sendchat.setUserName(senduser.getPublicName());
                // sendchat.setAt(now);
                // sendchat.setVisited(now);
                // return userChatInfoRepo.save(sendchat)
                // .flatMap(id->{
                //     UserChatInfo recievechat=new UserChatInfo();
                //     recievechat.setChatId(chatid);
                //     recievechat.setUserId(RecieverId);
    //                 recievechat.setChatName(senduser.getPublicName());
    //                 recievechat.setUserName(receiveuser.getPublicName());
    //                 recievechat.setAt(now);
    //                 recievechat.setVisited(now);
    //                 return userChatInfoRepo.save(recievechat).thenReturn("inserted");
    //             });
    //         });
    //     }))
    //     .switchIfEmpty(Mono.just("supplier"));
    // }

    public Mono<String> addchat(String SenderId,String RecieverId){
        System.out.println("chating");
        LocalDateTime now=LocalDateTime.now();
        return userInfoRepo.findByUserId(SenderId)
        .zipWith(userInfoRepo.findByUserId(RecieverId))
        .flatMap(tuple->{
            UserInfo senduser=tuple.getT1();
            UserInfo receiveuser=tuple.getT2();
            // return userChatInfoRepo.findIfExist(senduser.getUserId(), receiveuser.getPublicName(), receiveuser.getPrivateName())
             return userChatInfoRepo.findIfExistOrNot(SenderId, RecieverId)
             .flatMap(chatid->{
                System.out.println("exist");
                System.out.println(chatid);
                return Mono.just(chatid);
             })
             .switchIfEmpty(
             generateId()
            .flatMap(chatid->{
                String senderInitialName = senduser.getPublicName();
                if (senduser.getPrivateName() != null && !senduser.getPrivateName().trim().isEmpty()) {
                    if (UsernameSimilarityEvaluator.computeSelfSimilarity(senduser.getPublicName(), senduser.getPrivateName()) >= 3.0) {
                        senderInitialName = senduser.getPrivateName().trim();
                    }
                }

                String receiverInitialName = receiveuser.getPublicName();
                if (receiveuser.getPrivateName() != null && !receiveuser.getPrivateName().trim().isEmpty()) {
                    if (UsernameSimilarityEvaluator.computeSelfSimilarity(receiveuser.getPublicName(), receiveuser.getPrivateName()) >= 3.0) {
                        receiverInitialName = receiveuser.getPrivateName().trim();
                    }
                }

                String senderDisplayTitle = receiveuser.getPrivateName() != null && !receiveuser.getPrivateName().trim().isEmpty() ? receiveuser.getPrivateName() : receiveuser.getPublicName();
                String receiverDisplayTitle = senduser.getPrivateName() != null && !senduser.getPrivateName().trim().isEmpty() ? senduser.getPrivateName() : senduser.getPublicName();

                return userChatInfoRepo.insert(chatid, SenderId, senderDisplayTitle, senderInitialName, now, now,"private",null,receiveuser.getUserProfilePath(),senduser.getUserProfilePath())
                .then(userChatInfoRepo.insert(chatid, RecieverId, receiverDisplayTitle, receiverInitialName, now, now,"private",null,senduser.getUserProfilePath(),receiveuser.getUserProfilePath()))
                .thenReturn(chatid);
            }));
        });
    }

    public Mono<String> generateId0(){
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
        return idTableRepo.getChatid()
        .flatMap(chatid->{
            return numberToCode(chatid);
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

    public Mono<UserSearchResult> getInfo(String chatid,String userid){
        return userChatInfoRepo.getInfo(chatid, userid)
        .flatMap(user->{
            UserSearchResult User=new UserSearchResult();
            User.setUserId(user.getUserId());
            User.setUserName(user.getUserName());
            User.setRole(user.getRole());
            User.setProfile(user.getUserProfile());

            return userPulseStreakRepo.findById(user.getUserId())
                    .map(s -> s.getEffectiveStreak(LocalDate.now()))
                    .onErrorReturn(0)
                    .defaultIfEmpty(0)
                    .map(streak -> {
                        User.setPulseStreak(streak);
                        return User;
                    });
        });
    }
public Mono<String> updateInfo(UserSearchResult info, String Chatid) {
    if (info == null || info.getUserId() == null) {
        return Mono.just("failed");
    }

    return userInfoRepo.findByUserId(info.getUserId())
        .flatMap(user -> {
            String proposedName = info.getUserName() != null ? info.getUserName().trim() : null;

            Mono<Void> validationMono = Mono.empty();
            if (proposedName != null && !proposedName.isEmpty()) {
                // 1. Self-similarity check (>= 3.0 out of 10)
                double selfSim = UsernameSimilarityEvaluator.computeSelfSimilarity(user.getPublicName(), proposedName);
                if (selfSim < 3.0) {
                    return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chat name must have at least 3/10 similarity with your permanent username (@" + user.getPublicName() + ")"));
                }

                // 2. Peer collision & confusion check (< 8.0 out of 10)
                validationMono = userChatInfoRepo.findPeerUserNames(Chatid, info.getUserId())
                    .filter(peer -> peer != null && !peer.trim().isEmpty())
                    .flatMap(peer -> {
                        double peerSim = UsernameSimilarityEvaluator.computePeerSimilarity(proposedName, peer.trim());
                        if (peerSim >= 8.0) {
                            return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Chat name is too similar to participant '" + peer + "' in this chat (similarity: " + peerSim + "/10). It must be below 8/10."));
                        }
                        return Mono.empty();
                    })
                    .then();
            }

            return validationMono.then(userChatInfoRepo.findTypeByChatId(Chatid)
                .flatMap(type -> {
                    return userChatInfoRepo.getUsername(Chatid, info.getUserId())
                        .defaultIfEmpty("")
                        .flatMap(oldName -> {
                            boolean nameChanged = proposedName != null && !oldName.isEmpty() && !oldName.equals(proposedName);

                            boolean isCustomName = proposedName != null && (user.getPrivateName() == null || !proposedName.equals(user.getPrivateName().trim()));
                            boolean isCustomProfile = info.getProfile() != null && (user.getUserProfilePath() == null || !info.getProfile().equals(user.getUserProfilePath()));

                            Mono<Void> announceMono = nameChanged
                                ? myWebSocketHandler.announce(Chatid, "username_change", oldName, proposedName, info.getUserId())
                                : Mono.empty();

                            Mono<Void> updateUsername =
                                userChatInfoRepo.updateCustomChatInfo(proposedName, info.getProfile(), isCustomName, isCustomProfile, Chatid, info.getUserId())
                                .then(Mono.fromRunnable(() -> {
                                    if (proposedName != null) {
                                        myWebSocketHandler.updateUsernameCache(Chatid, info.getUserId(), proposedName);
                                    }
                                }))
                                .then(announceMono);

                            if ("private".equals(type)) {
                                return updateUsername
                                    .then(userChatInfoRepo.updateChatname(proposedName, info.getProfile(), Chatid, info.getUserId()))
                                    .thenReturn("ok");
                            } else {
                                return updateUsername.thenReturn("ok");
                            }
                        });
                }));
        })
        .onErrorResume(org.springframework.dao.DuplicateKeyException.class, e -> 
            Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "This name is already taken in this chat.")))
        .switchIfEmpty(Mono.just("failed"));
}


    public Mono<String> addspace(ChatSpace data) {
        return addspace(data, null, null);
    }

    public Mono<String> addspace(ChatSpace data, String userId, String userName) {
        return chatSpaceRepo.save(data)
            .flatMap(space -> {
                String safeChatId = space.getChatId();
                int spaceId = (space.getSpaceId() != null) ? space.getSpaceId().intValue() : 0;
                String spaceName = (space.getSpaceName() != null && !space.getSpaceName().trim().isEmpty())
                    ? space.getSpaceName().trim()
                    : "Space " + spaceId;

                Mono<String> resolveUserId;
                if (userId != null && !userId.trim().isEmpty() && !"server".equals(userId)) {
                    resolveUserId = Mono.just(userId.trim());
                } else {
                    resolveUserId = userChatInfoRepo.findUserIds(safeChatId).next().defaultIfEmpty("admin");
                }

                Mono<String> resolveUserName = (userName != null && !userName.trim().isEmpty())
                    ? Mono.just(userName.trim())
                    : (userId != null
                        ? userChatInfoRepo.getUsername(safeChatId, userId)
                            .switchIfEmpty(userInfoRepo.findByUserId(userId).map(UserInfo::getPublicName))
                            .defaultIfEmpty("Admin")
                        : Mono.just("Admin"));

                return Mono.zip(resolveUserId, resolveUserName)
                    .flatMap(tuple -> myWebSocketHandler.announce(safeChatId, "space_created", spaceName, tuple.getT2(), tuple.getT1(), spaceId))
                    .onErrorResume(err -> {
                        System.err.println("Failed to announce space creation: " + err.getMessage());
                        return Mono.empty();
                    })
                    .thenReturn("success");
            })
            .defaultIfEmpty("success")
            .onErrorResume(err -> {
                System.err.println("Failed in addspace: " + err.getMessage());
                return Mono.just("success");
            });
    }

    public Flux<ChatSpace> getspace(String chatid){
        return chatSpaceRepo.findByChatId(chatid);
    }
}
