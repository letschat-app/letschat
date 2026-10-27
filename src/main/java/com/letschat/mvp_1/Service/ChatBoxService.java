package com.letschat.mvp_1.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.ChatBoxReturnDTO;
import com.letschat.mvp_1.DTOs.LastMessageDTO;
import com.letschat.mvp_1.DTOs.LoadMessageDTO;
import com.letschat.mvp_1.Repositories.GroupInfoRepo;
import com.letschat.mvp_1.Repositories.MessageInfoRepo;
import com.letschat.mvp_1.Repositories.MessageTrackHistoryRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ChatBoxService {
    private final UserInfoRepo userInfoRepo;
    private final UserChatInfoRepo userChatInfoRepo;
    private final MessageInfoRepo messageInfoRepo;
    private final MessageTrackHistoryRepo messageTrackHistoryRepo;
    private final PollService pollService;

    public ChatBoxService(UserInfoRepo userInfoRepo, UserChatInfoRepo userChatInfoRepo, GroupInfoRepo groupInfoRepo,
                          MessageInfoRepo messageInfoRepo, MessageTrackHistoryRepo messageTrackHistoryRepo,
                          PollService pollService) {
        this.userInfoRepo = userInfoRepo;
        this.userChatInfoRepo = userChatInfoRepo;
        this.messageInfoRepo = messageInfoRepo;
        this.messageTrackHistoryRepo = messageTrackHistoryRepo;
        this.pollService = pollService;
    }

    public Flux<ChatBoxReturnDTO> getchat(String UserId) {
        Flux<ChatBoxReturnDTO> username = userChatInfoRepo.findUserChat(UserId);
        Flux<ChatBoxReturnDTO> groupname = userChatInfoRepo.findGroupChat(UserId);
        Flux<ChatBoxReturnDTO> roomname = userChatInfoRepo.findClassRoomChat(UserId);
        Flux<ChatBoxReturnDTO> allChats = Flux.concat(username, groupname, roomname);

        return messageInfoRepo.findLastMessageForUserChats(UserId)
            .collectMap(LastMessageDTO::getChatId, msg -> msg)
            .flatMapMany(lastMsgMap -> allChats.map(chat -> {
                LastMessageDTO lm = lastMsgMap.get(chat.getChatId());
                if (lm != null) {
                    chat.setLastMessage(lm.getContent());
                    chat.setLastMessageType(lm.getMessageType());
                    chat.setLastMessageSender(lm.getSenderName());
                    chat.setLastMessageSenderId(lm.getSenderId());
                    chat.setLastMessageTimestamp(lm.getTimestamp());
                    chat.setLastMessageId(lm.getMessageId());
                }
                return chat;
            }));
    }

    public Mono<List<Map<String, Object>>> getRecentMessagesForAllChats(String userId, int limit) {
        int messageLimit = limit > 0 ? limit : 20;
        return getchat(userId)
            .flatMap(chat -> {
                String chatId = chat.getChatId();
                String type = chat.getType();
                boolean isRoomOrClassroom = "classroom".equals(type) || "room".equals(type);
                Flux<LoadMessageDTO> msgsFlux = isRoomOrClassroom
                    ? messageInfoRepo.loadRecentForRoom(userId, chatId, 0, messageLimit)
                    : messageInfoRepo.loadRecent(userId, chatId, 0, messageLimit);

                return msgsFlux.collectList()
                    .flatMap(msgs -> pollService.enrichPollMessages(msgs, userId))
                    .map(msgs -> {
                        Map<String, Object> batch = new HashMap<>();
                        batch.put("type", "batch");
                        batch.put("chatid", chatId);
                        batch.put("hasmore", msgs.size() >= messageLimit);
                        batch.put("msgs", msgs);
                        return batch;
                    });
            })
            .collectList();
    }

    public Mono<Map<String, Object>> syncChatMessages(String userId, String chatId, Integer spaceId, java.time.LocalDateTime lastTimestamp) {
        int targetSpace = spaceId != null ? spaceId : 0;

        return userChatInfoRepo.findUserChat(userId)
            .concatWith(userChatInfoRepo.findGroupChat(userId))
            .concatWith(userChatInfoRepo.findClassRoomChat(userId))
            .filter(c -> chatId.equals(c.getChatId()))
            .next()
            .map(ChatBoxReturnDTO::getType)
            .defaultIfEmpty("private")
            .flatMap(chatType -> {
                boolean isRoomOrClassroom = "classroom".equals(chatType) || "room".equals(chatType);

                Mono<java.time.LocalDateTime> dbLatestMono = isRoomOrClassroom
                    ? messageInfoRepo.findLatestTimestampForRoom(chatId, targetSpace)
                    : messageInfoRepo.findLatestTimestamp(userId, chatId, targetSpace);

                return dbLatestMono
                    .defaultIfEmpty(java.time.LocalDateTime.MIN)
                    .flatMap(dbLatest -> {
                        boolean hasDbMessages = !dbLatest.equals(java.time.LocalDateTime.MIN);
                        boolean isUpToDate = lastTimestamp != null && hasDbMessages &&
                            (lastTimestamp.isEqual(dbLatest) || lastTimestamp.isAfter(dbLatest));

                        if (isUpToDate) {
                            Flux<LoadMessageDTO> msgsFlux = isRoomOrClassroom
                                ? messageInfoRepo.loadRecentForRoom(userId, chatId, targetSpace, 20)
                                : messageInfoRepo.loadRecent(userId, chatId, targetSpace, 20);

                            return msgsFlux.collectList()
                                .flatMap(msgs -> pollService.enrichPollMessages(msgs, userId))
                                .map(msgs -> {
                                    Map<String, Object> resp = new HashMap<>();
                                    resp.put("type", "batch");
                                    resp.put("chatid", chatId);
                                    resp.put("usermsgId", targetSpace);
                                    resp.put("hasmore", false);
                                    resp.put("isFullSync", false);
                                    resp.put("dbLatestTimestamp", hasDbMessages ? dbLatest : null);
                                    resp.put("msgs", msgs);
                                    return resp;
                                });
                        } else {
                            Flux<LoadMessageDTO> pendingFlux = isRoomOrClassroom
                                ? messageInfoRepo.loadPendingAfterForRoom(chatId, targetSpace, lastTimestamp, 50)
                                : messageInfoRepo.loadPendingAfter(userId, chatId, targetSpace, lastTimestamp, 50);

                            return pendingFlux.collectList()
                                .flatMap(msgs -> {
                                    if (msgs.isEmpty() && lastTimestamp == null) {
                                        Flux<LoadMessageDTO> initialFlux = isRoomOrClassroom
                                            ? messageInfoRepo.loadRecentForRoom(userId, chatId, targetSpace, 20)
                                            : messageInfoRepo.loadRecent(userId, chatId, targetSpace, 20);
                                        return initialFlux.collectList()
                                            .flatMap(m -> pollService.enrichPollMessages(m, userId))
                                            .map(m -> {
                                                Map<String, Object> resp = new HashMap<>();
                                                resp.put("type", "batch");
                                                resp.put("chatid", chatId);
                                                resp.put("usermsgId", targetSpace);
                                                resp.put("hasmore", false);
                                                resp.put("isFullSync", false);
                                                resp.put("dbLatestTimestamp", hasDbMessages ? dbLatest : null);
                                                resp.put("msgs", m);
                                                return resp;
                                            });
                                    }

                                    return pollService.enrichPollMessages(msgs, userId)
                                        .map(enrichedMsgs -> {
                                            Map<String, Object> resp = new HashMap<>();
                                            resp.put("type", "batch");
                                            resp.put("chatid", chatId);
                                            resp.put("usermsgId", targetSpace);
                                            resp.put("hasmore", enrichedMsgs.size() == 50);
                                            resp.put("isFullSync", true);
                                            resp.put("dbLatestTimestamp", hasDbMessages ? dbLatest : null);
                                            resp.put("msgs", enrichedMsgs);
                                            return resp;
                                        });
                                });
                        }
                    });
            });
    }

    public Mono<Map<String, Object>> getMessageTrackInfo(String messageId) {
        return messageTrackHistoryRepo.getmsgid(messageId)
            .flatMap(track -> {
                String recipientId = track.getRecieverId();
                return userInfoRepo.findById(recipientId)
                    .map(user -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("userId", recipientId);
                        map.put("userName", user.getUsername() != null ? user.getUsername() : user.getDisplayName());
                        map.put("profile", user.getUserProfilePath());
                        map.put("status", track.getStatus());
                        map.put("delivertime", track.getDeliverTime());
                        map.put("readtime", track.getReadTime());
                        map.put("mediaopenedtime", track.getMediaOpenedTime());
                        return map;
                    })
                    .defaultIfEmpty(new HashMap<>(Map.of(
                        "userId", recipientId,
                        "userName", recipientId,
                        "status", track.getStatus() != null ? track.getStatus() : "pending"
                    )));
            })
            .collectList()
            .map(recipients -> {
                Map<String, Object> result = new HashMap<>();
                result.put("msgid", messageId);
                List<String> readUsers = new java.util.ArrayList<>();
                List<String> deliveredUsers = new java.util.ArrayList<>();
                List<String> pendingUsers = new java.util.ArrayList<>();
                java.time.LocalDateTime maxDeliver = null;
                java.time.LocalDateTime maxRead = null;
                java.time.LocalDateTime maxMediaOpened = null;

                for (Map<String, Object> r : recipients) {
                    String uid = (String) r.get("userId");
                    String status = (String) r.get("status");
                    Object dt = r.get("delivertime");
                    Object rt = r.get("readtime");
                    Object mt = r.get("mediaopenedtime");
                    if (dt instanceof java.time.LocalDateTime) {
                        java.time.LocalDateTime ldt = (java.time.LocalDateTime) dt;
                        if (maxDeliver == null || ldt.isAfter(maxDeliver)) maxDeliver = ldt;
                    }
                    if (rt instanceof java.time.LocalDateTime) {
                        java.time.LocalDateTime ldt = (java.time.LocalDateTime) rt;
                        if (maxRead == null || ldt.isAfter(maxRead)) maxRead = ldt;
                    }
                    if (mt instanceof java.time.LocalDateTime) {
                        java.time.LocalDateTime ldt = (java.time.LocalDateTime) mt;
                        if (maxMediaOpened == null || ldt.isAfter(maxMediaOpened)) maxMediaOpened = ldt;
                    }

                    if ("read".equalsIgnoreCase(status) || "media-opened".equalsIgnoreCase(status)) {
                        readUsers.add(uid);
                    } else if ("delivered".equalsIgnoreCase(status)) {
                        deliveredUsers.add(uid);
                    } else {
                        pendingUsers.add(uid);
                    }
                }
                result.put("recipients", recipients);
                result.put("read_users", readUsers);
                result.put("delivered_users", deliveredUsers);
                result.put("pending_users", pendingUsers);
                result.put("delivertime", maxDeliver);
                result.put("readtime", maxRead);
                result.put("mediaopenedtime", maxMediaOpened);

                String overall = (!readUsers.isEmpty() && readUsers.size() == recipients.size()) ? "read" :
                    (!deliveredUsers.isEmpty() || !readUsers.isEmpty()) ? "delivered" : "pending";
                result.put("status", overall);
                return result;
            });
    }
}
