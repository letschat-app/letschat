package com.letschat.mvp_1.WebSocket;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.letschat.mvp_1.NotificationService;
import com.letschat.mvp_1.DTOs.ACKMessageDTO;
import com.letschat.mvp_1.DTOs.ChatUpdateDTO;
import com.letschat.mvp_1.DTOs.ReceivingMessageDTO;
import com.letschat.mvp_1.DTOs.SendingMessageDTO;
import com.letschat.mvp_1.Models.ScheduleMessage;
import com.letschat.mvp_1.Repositories.MessageInfoRepo;
import com.letschat.mvp_1.Repositories.MessageTrackHistoryRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;
import com.letschat.mvp_1.Service.AdminService;
import com.letschat.mvp_1.Service.PollService;
import org.springframework.transaction.reactive.TransactionalOperator;
import com.letschat.mvp_1.DTOs.PollUpdateDTO;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Component
public class MyWebSocketHandler implements WebSocketHandler{

    private final UserChatInfoRepo userChatInfoRepo;
    private final MessageInfoRepo messageInfoRepo;
    private final MessageTrackHistoryRepo messageTrackHistoryRepo;
    private final UserInfoRepo userInfoRepo;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final AdminService adminService;
    private final PollService pollService;
    private final TransactionalOperator transactionalOperator;

    public MyWebSocketHandler(UserChatInfoRepo userChatInfoRepo,UserInfoRepo userInfoRepo,ObjectMapper objectMapper,MessageInfoRepo messageInfoRepo,MessageTrackHistoryRepo messageTrackHistoryRepo,NotificationService notificationService,AdminService adminService,PollService pollService,TransactionalOperator transactionalOperator){
        this.userChatInfoRepo=userChatInfoRepo;
        this.userInfoRepo=userInfoRepo;
        this.objectMapper=objectMapper;
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.messageTrackHistoryRepo=messageTrackHistoryRepo;
        this.messageInfoRepo=messageInfoRepo;
        this.notificationService=notificationService;
        this.adminService=adminService;
        this.pollService=pollService;
        this.transactionalOperator=transactionalOperator;
    }
    private final ConcurrentHashMap<String,ConcurrentHashMap<String,String>> userstatus=new ConcurrentHashMap<>();
    private final Map<String,String>userid=new ConcurrentHashMap<>();
    private final Map<String,Sinks.Many<String>>usersink=new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String,ConcurrentHashMap<String,String>> username=new ConcurrentHashMap<>();
    //chatid<userid,username>
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<String>> userchats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> chatypes=new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> chatLastAccessed = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> msgSenderCache = new ConcurrentHashMap<>();
  //  private final Map<String, Set<String>> tokenCache = new ConcurrentHashMap<>();

    public int getConnectedWebSocketsCount() {
        return usersink.size();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session){
        String sessionid = session.getId();

        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer(256, false);

        String qry = session.getHandshakeInfo().getUri().getQuery();
        String id = null;
        if (qry != null && qry.contains("userid=")) {
            id = qry.split("userid=")[1].split("&")[0];
        }

        if (id == null) {
            return session.close();
        }

        System.out.println("connected:" + id);

        final String finalId = id; 

    usersink.compute(id, (key, oldSink) -> {
        if (oldSink != null) {
            System.out.println("removing old");
            oldSink.tryEmitComplete();
            userid.entrySet().removeIf(entry -> entry.getValue().equals(finalId)); 
        }
        return sink;
    });

    adminService.onUserConnected(id);
    userid.put(sessionid, id);

    System.out.println(userid.size() + "," + usersink.size());

    Mono<Void> connect = onconnect(id).then();

    Mono<Void> recieve = session.receive()
        .timeout(Duration.ofSeconds(60))
        .onErrorResume(e -> {
            System.out.println("checkin timeout: " + e.getMessage());
            return Mono.empty();
        })
        .flatMap(msg -> {
            String payload = msg.getPayloadAsText();
            if ("ping".equals(payload)) {
                sink.tryEmitNext("pong");
                return Mono.empty();
            }
            return onrecieve(sessionid, payload)
            .onErrorResume(err->{
                System.out.println("error in recive msgs: "+err.getMessage());
                err.printStackTrace();
                return Mono.empty();
            });
        },32)
        .doOnError(err -> System.out.println("error:" + err.getMessage()))
        .doFinally(status -> {
            String uid = userid.remove(sessionid);
            System.out.println(userid.size());
            if (uid != null) { 
                adminService.onUserDisconnected(uid);
                usersink.computeIfPresent(uid, (key, currentSink) -> {
                    if (currentSink == sink) {
                        currentSink.tryEmitComplete();
                        System.out.println("closed");
                        return null;
                    }
                    return currentSink;
                });
            }
            System.out.println("removed");
        })
        .then();

    Mono<Void> send = session.send(
        sink.asFlux()
            .doOnSubscribe(sub -> System.out.println("to send " + userid.get(sessionid)))
            .doOnCancel(() -> System.out.println("to cancel " + userid.get(sessionid)))
            .doOnNext(msg -> System.out.println("sending to " + userid.get(sessionid) + " : " + msg))
            .map(session::textMessage)
    );

   // return connect.then(Mono.when(recieve, send));
    return Mono.when(connect, recieve, send);
}
    public Mono<String> getusername(String chatId,String userId){
        System.out.println(chatId+userId);
        return Mono.defer(() -> {
            chatLastAccessed.put(chatId, System.currentTimeMillis());
            ConcurrentHashMap<String, String> userMap = username.computeIfAbsent(chatId, k -> new ConcurrentHashMap<>());
            String cachedUsername = userMap.get(userId);
            if (cachedUsername != null) {
                System.out.println("exist in cache"+cachedUsername);
                return Mono.just(cachedUsername);
            }
            System.out.println("db call"+userId);
            return userChatInfoRepo.getUsername(chatId,userId)
            .doOnNext(name -> {userMap.put(userId, name);System.out.println(name);});
        });
    }

public Mono<Map<String,Boolean>> checkUserStatus(List<String> userids){
    return Mono.just(userids.stream() .collect(Collectors.toMap( id -> id, usersink::containsKey )));
}

public void updateUsernameCache(String chatId, String userId, String newName) {
    chatLastAccessed.put(chatId, System.currentTimeMillis());
    Map<String, String> chatMap = username.computeIfAbsent(chatId, k -> new ConcurrentHashMap<>());
    chatMap.put(userId, newName);
}

public void removeUsernameCache(String chatId, String userId) {

    ConcurrentHashMap<String, String> map = username.get(chatId);

    if (map != null) {
        map.remove(userId);
        if (map.isEmpty()) {
            username.remove(chatId);
        }
    }
}

public void invalidateUserCache(String userId) {
    if (userId == null) return;
    username.values().forEach(map -> map.remove(userId));
}

public void invalidateChatMembers(String chatId) {
    if (chatId == null) return;
    userchats.remove(chatId);
    chatypes.remove(chatId);
    username.remove(chatId);
}

public Flux<String> getUserIds(String chatId, String userId) {

    return Flux.defer(() -> {
        chatLastAccessed.put(chatId, System.currentTimeMillis());
        CopyOnWriteArrayList<String> cached = userchats.get(chatId);

        if (cached != null) {
            System.out.println(
                "[CACHE HIT] chatId=" + chatId +
                " sender=" + userId +
                " cachedUsers=" + cached
            );
            return Flux.fromIterable(cached)
                       .filter(id -> !id.equals(userId));
        }

        // DB returns Flux<String>
        return userChatInfoRepo.findUserIds(chatId) // Flux<String>
                .collectList()                      // materialize ONCE
                .map(CopyOnWriteArrayList::new)     // thread-safe cache
                .doOnNext(listFromDb -> {
                    System.out.println(
                        "[DB RESULT] chatId=" + chatId +
                        " usersFromDb=" + listFromDb
                    );

                    userchats.put(chatId, listFromDb);

                    System.out.println(
                        "[CACHE STORED] chatId=" + chatId +
                        " cachedUsers=" + userchats.get(chatId)
                    );
                })
                .flatMapMany(list ->
                        Flux.fromIterable(list)
                            .filter(id -> !id.equals(userId))
                );
    });
}

public void addUserToChatCache(String chatId, String userId) {
    chatLastAccessed.put(chatId, System.currentTimeMillis());
    userchats.computeIfAbsent(chatId, k -> new CopyOnWriteArrayList<>())
             .addIfAbsent(userId);
}

public void removeUserFromChatCache(String chatId, String userId) {

    CopyOnWriteArrayList<String> users = userchats.get(chatId);

    if (users != null) {
        users.remove(userId);

        if (users.isEmpty()) {
            userchats.remove(chatId);
        }
    }
}


    public Mono<String> getType(String chatid){
        if (chatid == null) return Mono.error(new RuntimeException("chatid is null"));
        return Mono.defer(()->{
            chatLastAccessed.put(chatid, System.currentTimeMillis());
            String type=chatypes.get(chatid);
            if(type != null){
                return Mono.just(type);
            }

            return userChatInfoRepo.findTypeByChatId(chatid)
            .doOnNext(type1->chatypes.putIfAbsent(chatid,type1))
            .defaultIfEmpty("private");
        });
    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void cleanIdleCaches() {
        long now = System.currentTimeMillis();
        long idleTtl = 60 * 60 * 1000L; // 1 hour

        // 1. Clean "in-active" entries and empty/offline maps from userstatus
        userstatus.entrySet().removeIf(entry -> {
            String uid = entry.getKey();
            ConcurrentHashMap<String, String> chatMap = entry.getValue();
            if (chatMap == null) return true;
            chatMap.entrySet().removeIf(e -> !"active".equals(e.getValue()));
            return chatMap.isEmpty() || !usersink.containsKey(uid);
        });

        // 2. Evict chats idle for > 1 hour from username, userchats, and chatypes
        chatLastAccessed.entrySet().removeIf(entry -> {
            if ((now - entry.getValue()) > idleTtl) {
                String chatId = entry.getKey();
                username.remove(chatId);
                userchats.remove(chatId);
                chatypes.remove(chatId);
                return true;
            }
            return false;
        });
    }



public Mono<Void> onrecieve(String sessionId, String json) {

    return Mono.fromCallable(() -> objectMapper.readTree(json))
        .flatMap(node -> {
            if (node.has("purpose")) {
                return loadmsg(sessionId, json);
            }

            ReceivingMessageDTO data;
            try {
                data = objectMapper.treeToValue(node, ReceivingMessageDTO.class);
            } catch (JsonProcessingException e) {
                System.out.println("load msg calling");
                return loadmsg(sessionId, json);
            }

            if (data.getchatid() == null) {
                return Mono.error(new RuntimeException("ChatId is null"));
            }

            if (data.gettimestamp() == null) {
                data.settimestamp(LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")));
            }

            String msgId = "Msg-" + UUID.randomUUID();
            String chatId = data.getchatid();
            String userId = this.userid.get(sessionId);

            if (userId == null) {
                return Mono.error(new RuntimeException("Invalid session"));
            }

            Sinks.Many<String> senderSink = usersink.get(userId);

            // INDICATOR
            if ("indicator".equals(data.gettype())) {

                return getusername(chatId, userId)
                    .flatMap(username ->
                        getUserIds(chatId, userId)
                            .flatMap(receiverId -> {

                                Sinks.Many<String> receiverSink =
                                    usersink.get(receiverId);

                                SendingMessageDTO msg =
                                    buildMsg(
                                        null,
                                        chatId,
                                        data,
                                        username,
                                        userId
                                    );

                                return sendToSinkAsync(receiverSink, msg);

                            })
                            .then()
                    );
            }

            // NORMAL FLOW
            if ("poll".equals(data.gettype())) {
                Long pollId;
                try {
                    JsonNode pollNode = objectMapper.readTree(data.getcontent());
                    pollId = (pollNode.has("poll_id") && !pollNode.get("poll_id").isNull()) ? pollNode.get("poll_id").asLong() : null;
                    if (pollId == null) {
                        return sendAckAsync(senderSink, chatId, data.gettempmsgid(), null, "error");
                    }
                } catch (Exception e) {
                    return sendAckAsync(senderSink, chatId, data.gettempmsgid(), null, "error");
                }

                return getType(chatId)
                    .flatMap(type ->
                        getusername(chatId, userId)
                            .flatMap(username -> {
                                Mono<Void> insertAndLink = messageInfoRepo.insert(
                                    msgId,
                                    chatId,
                                    userId,
                                    data.gettype(),
                                    data.getcontent(),
                                    data.getrepliedto(),
                                    data.getforwardedfrom(),
                                    data.gettimestamp(),
                                    data.getspaceid()
                                )
                                .then(pollService.linkPollToMessage(pollId, msgId, userId, chatId));

                                return insertAndLink
                                    .as(transactionalOperator::transactional)
                                    .then(pollService.buildPollUpdateDTO(pollId, msgId, userId))
                                    .flatMap(pollDTO -> {
                                        SendingMessageDTO msg = buildMsg(msgId, chatId, data, username, userId);
                                        msg.setpoll(pollDTO);

                                        return getUserIds(chatId, userId)
                                            .<MessageResult>flatMap(receiverId ->
                                                sendMessageWithPoll(receiverId, chatId, userId, username, type, msg)
                                            )
                                            .collectList()
                                            .flatMap(results -> {
                                                String finalStatus = "read";
                                                for (MessageResult result : results) {
                                                    if ("pending".equals(result.status())) {
                                                        finalStatus = "pending";
                                                        break;
                                                    }
                                                    if ("delivered".equals(result.status())) {
                                                        finalStatus = "delivered";
                                                    }
                                                }

                                                List<String> receiverIds = results.stream().map(MessageResult::receiverId).toList();
                                                List<String> statuses = results.stream().map(MessageResult::status).toList();
                                                List<LocalDateTime> deliveredTimes = results.stream().map(MessageResult::deliveredTime).toList();

                                                List<String> pendingUsers = results.stream().filter(r -> "pending".equals(r.status())).map(MessageResult::receiverId).toList();
                                                List<String> deliveredUsers = results.stream().filter(r -> "delivered".equals(r.status())).map(MessageResult::receiverId).toList();
                                                List<String> readUsers = results.stream().filter(r -> "read".equals(r.status())).map(MessageResult::receiverId).toList();

                                                return sendAckAsync(
                                                    senderSink,
                                                    chatId,
                                                    data.gettempmsgid(),
                                                    msgId,
                                                    finalStatus,
                                                    pendingUsers,
                                                    deliveredUsers,
                                                    readUsers,
                                                    LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"))
                                                )
                                                .then(messageTrackHistoryRepo.batchInsert(msgId, userId, receiverIds, statuses, deliveredTimes));
                                            });
                                    });
                            })
                    )
                    .then()
                    .onErrorResume(e -> sendAckAsync(senderSink, chatId, data.gettempmsgid(), null, "error"));
            }
            return getType(chatId)
                .flatMap(type ->
                    getusername(chatId, userId)
                        .flatMap(username ->
                            getUserIds(chatId, userId)
                                .<MessageResult>flatMap(receiverId ->
                                    sendMessage(
                                        receiverId,
                                        chatId,
                                        userId,
                                        username,
                                        type,
                                        data,
                                        msgId
                                    )
                                )
                                .collectList()
                                .flatMap(results -> {

                                    String finalStatus = "read";

                                    for (MessageResult result : results) {

                                        if ("pending".equals(result.status())) {
                                            finalStatus = "pending";
                                            break;
                                        }

                                        if ("delivered".equals(result.status())) {
                                            finalStatus = "delivered";
                                        }
                                    }

                                    // SEND ACK AFTER MESSAGE
                                    List<String> receiverIds = results.stream()
                                        .map(MessageResult::receiverId)
                                        .toList();

                                    List<String> statuses = results.stream()
                                        .map(MessageResult::status)
                                        .toList();

                                    List<LocalDateTime> deliveredTimes = results.stream()
                                        .map(MessageResult::deliveredTime)
                                        .toList();

                                    List<String> pendingUsers = results.stream()
                                        .filter(r -> "pending".equals(r.status()))
                                        .map(MessageResult::receiverId)
                                        .toList();

                                    List<String> deliveredUsers = results.stream()
                                        .filter(r -> "delivered".equals(r.status()))
                                        .map(MessageResult::receiverId)
                                        .toList();

                                    List<String> readUsers = results.stream()
                                        .filter(r -> "read".equals(r.status()))
                                        .map(MessageResult::receiverId)
                                        .toList();

                                    LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

                                    return sendAckAsync(
                                        senderSink,
                                        chatId,
                                        data.gettempmsgid(),
                                        msgId,
                                        finalStatus,
                                        pendingUsers,
                                        deliveredUsers,
                                        readUsers,
                                        now
                                    )
                                    .then(
                                        messageInfoRepo.insert(
                                            msgId,
                                            chatId,
                                            userId,
                                            data.gettype(),
                                            data.getcontent(),
                                            data.getrepliedto(),
                                            data.getforwardedfrom(),
                                            data.gettimestamp(),
                                            data.getspaceid()
                                        )
                                    )
                                    .then(
                                        messageTrackHistoryRepo.batchInsert(
                                            msgId,
                                            userId,
                                            receiverIds,
                                            statuses,
                                            deliveredTimes
                                        )
                                    );
                                })
                        )
                )
                .then()
                .doOnSuccess(v -> {
                    adminService.onMessageSent();
                    adminService.onUserActivity(userId);
                });
        })
        .onErrorResume(e -> {
            System.out.println("load msg calling");
            return loadmsg(sessionId, json);
        })
        .then();
}


private Mono<MessageResult> sendMessage(
        String receiverId,
        String chatId,
        String userId,
        String username,
        String type,
        ReceivingMessageDTO data,
        String msgId
) {

    System.out.println(
        "sednerid:" + userId +
        "sendername:" + username
    );

    msgSenderCache.put(msgId, userId);

    Sinks.Many<String> receiverSink = usersink.get(receiverId);

    SendingMessageDTO msg =
        buildMsg(
            msgId,
            chatId,
            data,
            username,
            userId
        );

    boolean isReceiverActive = receiverSink != null && "active".equals(
        userstatus.getOrDefault(receiverId, new ConcurrentHashMap<>()).get(chatId)
    );

    if (!isReceiverActive) {
        fireNotification(
            receiverId,
            chatId,
            userId,
            username,
            data,
            type,
            msgId
        );
    }

    // CLASSROOM / ROOM
    if ("classroom".equals(type) || "room".equals(type)) {

        if (receiverSink != null) {

            return sendToSinkAsync(
                receiverSink,
                msg
            ).thenReturn(
                new MessageResult(
                    receiverId,
                    "read",
                    null
                )
            );
        }

        return Mono.just(
            new MessageResult(
                receiverId,
                "read",
                null
            )
        );
    }

    // NORMAL CHAT
    if (receiverSink != null) {

        userstatus
            .computeIfAbsent(
                receiverId,
                k -> new ConcurrentHashMap<>()
            )
            .putIfAbsent(
                chatId,
                "in-active"
            );

        boolean isActive =
            "active".equals(
                userstatus
                    .getOrDefault(
                        receiverId,
                        new ConcurrentHashMap<>()
                    )
                    .get(chatId)
            );

        String status =
            isActive
                ? "read"
                : "delivered";

        LocalDateTime deliveredTime =
            LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

        return sendToSinkAsync(
            receiverSink,
            msg
        ).thenReturn(
            new MessageResult(
                receiverId,
                status,
                deliveredTime
            )
        );
    }

    // OFFLINE
    return Mono.just(
        new MessageResult(
            receiverId,
            "pending",
            null
        )
    );
}

private Mono<MessageResult> sendMessageWithPoll(
        String receiverId,
        String chatId,
        String userId,
        String username,
        String type,
        SendingMessageDTO msg
) {
    Sinks.Many<String> receiverSink = usersink.get(receiverId);

    ReceivingMessageDTO tempDTO = new ReceivingMessageDTO();
    tempDTO.setcontent(msg.getcontent());
    tempDTO.settype(msg.gettype());
    tempDTO.setspaceid(msg.getspaceid());

    boolean isReceiverActive = receiverSink != null && "active".equals(
        userstatus.getOrDefault(receiverId, new ConcurrentHashMap<>()).get(chatId)
    );

    if (!isReceiverActive) {
        fireNotification(receiverId, chatId, userId, username, tempDTO, type, msg.getmsgid());
    }

    if (receiverSink != null) {
        userstatus.computeIfAbsent(receiverId, k -> new ConcurrentHashMap<>()).putIfAbsent(chatId, "in-active");
        boolean isActive = "active".equals(userstatus.getOrDefault(receiverId, new ConcurrentHashMap<>()).get(chatId));
        String status = isActive ? "read" : "delivered";
        LocalDateTime deliveredTime = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

        return sendToSinkAsync(receiverSink, msg).thenReturn(new MessageResult(receiverId, status, deliveredTime));
    }

    return Mono.just(new MessageResult(receiverId, "pending", null));
}


private record MessageResult(
    String receiverId,
    String status,
    LocalDateTime deliveredTime
) {}

// ── helpers ───────────────────────────────────────────────────────────────────

private SendingMessageDTO buildMsg(String msgId, String chatId,
                                   ReceivingMessageDTO data,
                                   String username, String userId) {
    SendingMessageDTO msg = new SendingMessageDTO();
    msg.setmsgid(msgId);
    msg.setchatid(chatId);
    msg.settype(data.gettype());
    msg.setcontent(data.getcontent());
    msg.setsendername(username);
    msg.setsenderid(userId);
    msg.settimestamp(data.gettimestamp());
    msg.setrepliedto(data.getrepliedto());
    msg.setforwardedfrom(data.getforwardedfrom());
    msg.setspaceid(data.getspaceid());
    return msg;
}

private Mono<Void> sendToSinkAsync(Sinks.Many<String> sink, Object payload) {
    if (sink == null) return Mono.empty();
    return Mono.fromCallable(() -> objectMapper.writeValueAsString(payload))
        .doOnNext(sink::tryEmitNext)
        .then();
}

private Mono<Void> sendAckAsync(Sinks.Many<String> senderSink, String chatId,
                                 String tempMsgId, String msgId, String status) {
    ACKMessageDTO ack = new ACKMessageDTO();
    ack.setchatid(chatId);
    ack.settempmsgid(tempMsgId);
    ack.setmsgid(msgId);
    ack.setstatus(status);
    return sendToSinkAsync(senderSink, ack);
}

private Mono<Void> sendAckAsync(Sinks.Many<String> senderSink, String chatId,
                                 String tempMsgId, String msgId, String status,
                                 List<String> pendingUsers, List<String> deliveredUsers, List<String> readUsers,
                                 LocalDateTime timestamp) {
    ACKMessageDTO ack = new ACKMessageDTO();
    ack.setchatid(chatId);
    ack.settempmsgid(tempMsgId);
    ack.setmsgid(msgId);
    ack.setstatus(status);
    if (pendingUsers != null && !pendingUsers.isEmpty()) ack.setpending_users(pendingUsers);
    if (deliveredUsers != null && !deliveredUsers.isEmpty()) ack.setdelivered_users(deliveredUsers);
    if (readUsers != null && !readUsers.isEmpty()) ack.setread_users(readUsers);
    ack.settimestamp(timestamp);
    return sendToSinkAsync(senderSink, ack);
}

private String deriveChatKind(String chatId, String type) {
    if ((type != null && "room".equalsIgnoreCase(type)) || (chatId != null && chatId.startsWith("room_"))) {
        return "room";
    }
    if (type != null && "private".equalsIgnoreCase(type)) {
        return "dm";
    }
    if (type != null && ("group".equalsIgnoreCase(type) || "classroom".equalsIgnoreCase(type))) {
        java.util.concurrent.CopyOnWriteArrayList<String> members = userchats.get(chatId);
        if (members != null && members.size() == 2) {
            return "dm";
        }
        return "group";
    }
    return "group";
}

private void fireNotification(String receiverId, String chatId, String senderId,
                               String username, ReceivingMessageDTO data, String type, String msgId) {
    if (data == null || "indicator".equalsIgnoreCase(data.gettype())) {
        return;
    }

    String effectiveMsgId = (msgId != null && !msgId.trim().isEmpty())
            ? msgId.trim()
            : chatId + "-" + System.currentTimeMillis();

    String content = data.getcontent() != null ? data.getcontent() : "";
    String msgType = (data.gettype() != null && !data.gettype().trim().isEmpty()) ? data.gettype() : "text";
    String spaceId = data.getspaceid() != null ? String.valueOf(data.getspaceid()) : "0";
    String repliedTo = data.getrepliedto();

    String payload = spaceId + "/%20/" + msgType + "/%20/" + username + "/%20/" + content;
    String chatKind = deriveChatKind(chatId, type);

    boolean isMention = content.contains("<@" + receiverId + ":") || content.contains("<@" + receiverId + ">");

    Mono<Boolean> isReplyToUserMono;
    if (repliedTo != null && !repliedTo.trim().isEmpty()) {
        String cachedSender = msgSenderCache.get(repliedTo);
        if (cachedSender != null) {
            isReplyToUserMono = Mono.just(receiverId.equals(cachedSender));
        } else {
            isReplyToUserMono = userChatInfoRepo.findSenderInChat(repliedTo)
                .map(senderInfo -> {
                    if (senderInfo != null && senderInfo.getSenderId() != null) {
                        msgSenderCache.put(repliedTo, senderInfo.getSenderId());
                        return receiverId.equals(senderInfo.getSenderId());
                    }
                    return false;
                })
                .defaultIfEmpty(false);
        }
    } else {
        isReplyToUserMono = Mono.just(false);
    }

    userChatInfoRepo.findChatNameByChatIdAndUserId(chatId, receiverId)
        .switchIfEmpty(userChatInfoRepo.findChatNameByChatId(chatId))
        .defaultIfEmpty(username)
        .flatMap(chatName ->
            isReplyToUserMono.flatMap(isReply -> {
                String alertKind = isMention ? "mention" : (isReply ? "reply" : "message");
                String legacyTitle;
                if ("mention".equals(alertKind)) {
                    legacyTitle = "dm".equals(chatKind) ? username : username + " mentioned you in " + chatName;
                } else if ("reply".equals(alertKind)) {
                    legacyTitle = "dm".equals(chatKind) ? username : username + " replied to you in " + chatName;
                } else if ("room".equals(chatKind)) {
                    legacyTitle = "New messages in " + chatName;
                } else {
                    legacyTitle = "dm".equals(chatKind) ? username : chatName;
                }

                return notificationService.notifyUser(
                        receiverId,
                        chatId,
                        legacyTitle,
                        chatName,
                        chatKind,
                        alertKind,
                        senderId,
                        username,
                        msgType,
                        effectiveMsgId,
                        spaceId,
                        content,
                        System.currentTimeMillis(),
                        isMention,
                        isReply,
                        payload,
                        repliedTo
                );
            })
        )
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> {
            System.out.println("FCM error: " + e);
            return Mono.empty();
        })
        .subscribe();
}

    @SuppressWarnings("ConvertToStringSwitch")
    public Mono<Void> loadmsg(String sessionid, String json) {
        System.out.println("load msg called");
        String Userid = userid.get(sessionid);
        if (Userid == null) {
            System.out.println("loadmsg: session not found " + sessionid);
            return Mono.empty();
        }
        Sinks.Many<String> sender_sink = usersink.get(Userid);
        if (sender_sink == null) {
            System.out.println("loadmsg: sink not found for " + Userid);
            return Mono.empty();
        }
        return Mono.fromCallable(() -> objectMapper.readValue(json, ChatUpdateDTO.class))
            .flatMap(data -> {
                String purpose = data.getpurpose();
                if (purpose == null) return Mono.empty();

                return getType(data.getuserchatid())
                    .flatMap(type -> {
                        boolean isRoomOrClassroom = "classroom".equals(type) || "room".equals(type);
                        String chatId = data.getuserchatid();

                        switch (purpose) {
                            case "load": {
                                LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
                                if (!isRoomOrClassroom) {
                                    userstatus.compute(Userid, (key, chatmap) -> {
                                        if (chatmap == null) chatmap = new ConcurrentHashMap<>();
                                        chatmap.put(chatId, "active");
                                        return chatmap;
                                    });
                                }
                                int space = 0;
                                if (data.getmsgid() != null && !data.getmsgid().trim().isEmpty()) {
                                    try {
                                        space = Integer.parseInt(data.getmsgid().trim());
                                    } catch (Exception e) {
                                        space = 0;
                                    }
                                }
                                notificationService.clear(Userid, chatId);

                                Mono<Void> checkInMono = isRoomOrClassroom ? Mono.empty() :
                                    messageTrackHistoryRepo.updateoncheckin(Userid, chatId, now, space)
                                        .collectList()
                                        .flatMap(updatedMsgs -> {
                                            if (updatedMsgs.isEmpty()) return Mono.empty();
                                            Map<String, List<String>> senderMsgMap = updatedMsgs.stream()
                                                .collect(Collectors.groupingBy(
                                                    com.letschat.mvp_1.Models.MessageTrackHistory::getSenderId,
                                                    Collectors.mapping(com.letschat.mvp_1.Models.MessageTrackHistory::getMessageId, Collectors.toList())
                                                ));
                                            return Flux.fromIterable(senderMsgMap.entrySet())
                                                .flatMap(entry -> {
                                                    String senderId = entry.getKey();
                                                    List<String> msgIds = entry.getValue();
                                                    if (usersink.containsKey(senderId)) {
                                                        ACKMessageDTO ack = new ACKMessageDTO(chatId, msgIds, "read", now);
                                                    ack.setread_users(java.util.Collections.singletonList(Userid));
                                                        ack.setread_users(java.util.Collections.singletonList(Userid));
                                                        Sinks.Many<String> ackSink = usersink.get(senderId);
                                                        return sendToSinkAsync(ackSink, ack);
                                                    }
                                                    return Mono.empty();
                                                })
                                                .then();
                                        });

                                Flux<com.letschat.mvp_1.DTOs.LoadMessageDTO> msgsFlux = isRoomOrClassroom ?
                                        messageInfoRepo.loadforroom(Userid, chatId, space, now) :
                                        messageInfoRepo.load(Userid, chatId, space, now);

                                return checkInMono.then(
                                    msgsFlux.collectList()
                                        .flatMap(msgs -> pollService.enrichPollMessages(msgs, Userid))
                                        .flatMap(msgs -> {
                                            Map<String, Object> batch = new HashMap<>();
                                            batch.put("type", "batch");
                                            batch.put("chatid", chatId);
                                            batch.put("hasmore", msgs.size() == 60);
                                            batch.put("msgs", msgs);
                                            return Mono.fromCallable(() -> objectMapper.writeValueAsString(batch))
                                                .doOnNext(sender_sink::tryEmitNext)
                                                .onErrorResume(e -> {
                                                    System.out.println(e);
                                                    return Mono.empty();
                                                });
                                        }).then()
                                );
                            }
                            case "load-more": {
                                int space = 0;
                                if (data.getmsgid() != null && !data.getmsgid().trim().isEmpty()) {
                                    try {
                                        space = Integer.parseInt(data.getmsgid().trim());
                                    } catch (Exception e) {
                                        space = 0;
                                    }
                                }
                                LocalDateTime before = data.gettime() != null ? data.gettime() : LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
                                Flux<com.letschat.mvp_1.DTOs.LoadMessageDTO> msgsFlux = isRoomOrClassroom ?
                                        messageInfoRepo.loadforroom(Userid, chatId, space, before) :
                                        messageInfoRepo.load(Userid, chatId, space, before);

                                return msgsFlux.collectList()
                                    .flatMap(msgs -> pollService.enrichPollMessages(msgs, Userid))
                                    .flatMap(msgs -> {
                                        Map<String, Object> batch = new HashMap<>();
                                        batch.put("type", "batch");
                                        batch.put("chatid", chatId);
                                        batch.put("hasmore", msgs.size() == 60);
                                        batch.put("msgs", msgs);
                                        notificationService.clear(Userid, chatId);
                                        return Mono.fromCallable(() -> objectMapper.writeValueAsString(batch))
                                            .doOnNext(sender_sink::tryEmitNext)
                                            .onErrorResume(e -> {
                                                System.out.println(e);
                                                return Mono.empty();
                                            });
                                    }).then();
                            }
                            case "vote-poll": {
                                String msgId = data.getmsgid();
                                List<Long> optionIds = new ArrayList<>();
                                Long pollId = null;

                                try {
                                    JsonNode node = objectMapper.readTree(json);
                                    if (node.has("option_ids") && node.get("option_ids").isArray()) {
                                        for (JsonNode item : node.get("option_ids")) {
                                            optionIds.add(item.asLong());
                                        }
                                    }
                                    if (node.has("poll_id")) {
                                        pollId = node.get("poll_id").asLong();
                                    }
                                } catch (Exception e) {}

                                return pollService.votePoll(Userid, chatId, msgId, optionIds, pollId)
                                    .flatMap(pollDTO -> {
                                        Mono<Void> sendToVoter = sendToSinkAsync(sender_sink, pollDTO);

                                        PollUpdateDTO publicDTO = new PollUpdateDTO();
                                        publicDTO.setType("poll-update");
                                        publicDTO.setChatid(pollDTO.getChatid());
                                        publicDTO.setMsgid(pollDTO.getMsgid());
                                        publicDTO.setPoll_id(pollDTO.getPoll_id());
                                        publicDTO.setQuestion(pollDTO.getQuestion());
                                        publicDTO.setAllow_multiple(pollDTO.getAllow_multiple());
                                        publicDTO.setAllow_user_options(pollDTO.getAllow_user_options());
                                        publicDTO.setAnonymous(pollDTO.getAnonymous());
                                        publicDTO.setIs_hidden_voting(pollDTO.getIs_hidden_voting());
                                        publicDTO.setIs_expired(pollDTO.getIs_expired());
                                        publicDTO.setExpires_at(pollDTO.getExpires_at());
                                        publicDTO.setTotal_voters(pollDTO.getTotal_voters());
                                        publicDTO.setTotal_members(pollDTO.getTotal_members());
                                        publicDTO.setOptions(pollDTO.getOptions());

                                        Mono<Void> broadcastToOthers = getUserIds(chatId, Userid)
                                            .flatMap(receiverId -> {
                                                Sinks.Many<String> receiverSink = usersink.get(receiverId);
                                                return sendToSinkAsync(receiverSink, publicDTO);
                                            })
                                            .then();

                                        return sendToVoter.then(broadcastToOthers);
                                    })
                                    .onErrorResume(e -> {
                                        System.out.println("vote-poll error: " + e.getMessage());
                                        Map<String, String> errMap = Map.of("error", e.getMessage(), "purpose", "vote-poll");
                                        return sendToSinkAsync(sender_sink, errMap);
                                    });
                            }
                            case "add-poll-option": {
                                String msgId = data.getmsgid();
                                String optionText = "";
                                Long pollId = null;

                                try {
                                    JsonNode node = objectMapper.readTree(json);
                                    if (node.has("option_text")) {
                                        optionText = node.get("option_text").asText();
                                    }
                                    if (node.has("poll_id")) {
                                        pollId = node.get("poll_id").asLong();
                                    }
                                } catch (Exception e) {}

                                return pollService.addPollOption(Userid, chatId, msgId, optionText, pollId)
                                    .flatMap(pollDTO -> {
                                        PollUpdateDTO publicDTO = new PollUpdateDTO();
                                        publicDTO.setType("poll-update");
                                        publicDTO.setChatid(pollDTO.getChatid());
                                        publicDTO.setMsgid(pollDTO.getMsgid());
                                        publicDTO.setPoll_id(pollDTO.getPoll_id());
                                        publicDTO.setQuestion(pollDTO.getQuestion());
                                        publicDTO.setAllow_multiple(pollDTO.getAllow_multiple());
                                        publicDTO.setAllow_user_options(pollDTO.getAllow_user_options());
                                        publicDTO.setAnonymous(pollDTO.getAnonymous());
                                        publicDTO.setIs_hidden_voting(pollDTO.getIs_hidden_voting());
                                        publicDTO.setIs_expired(pollDTO.getIs_expired());
                                        publicDTO.setExpires_at(pollDTO.getExpires_at());
                                        publicDTO.setTotal_voters(pollDTO.getTotal_voters());
                                        publicDTO.setTotal_members(pollDTO.getTotal_members());
                                        publicDTO.setOptions(pollDTO.getOptions());

                                        return getUserIds(chatId, Userid)
                                            .flatMap(receiverId -> {
                                                Sinks.Many<String> receiverSink = usersink.get(receiverId);
                                                return sendToSinkAsync(receiverSink, publicDTO);
                                            })
                                            .then(sendToSinkAsync(sender_sink, publicDTO));
                                    })
                                    .onErrorResume(e -> {
                                        System.out.println("add-poll-option error: " + e.getMessage());
                                        Map<String, String> errMap = Map.of("error", e.getMessage(), "purpose", "add-poll-option");
                                        return sendToSinkAsync(sender_sink, errMap);
                                    });
                            }
                            case "check-in": {
                                LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
                                System.out.println("check in " + chatId);
                                userstatus.compute(Userid, (key, chatmap) -> {
                                    if (chatmap == null) chatmap = new ConcurrentHashMap<>();
                                    chatmap.put(chatId, "active");
                                    return chatmap;
                                });
                                int space = 0;
                                if (data.getmsgid() != null && !data.getmsgid().trim().isEmpty()) {
                                    try {
                                        space = Integer.parseInt(data.getmsgid().trim());
                                    } catch (Exception e) {
                                        space = 0;
                                    }
                                }
                                notificationService.clear(Userid, chatId);

                                return messageTrackHistoryRepo.updateoncheckin(Userid, chatId, now, space)
                                    .collectList()
                                    .flatMap(updatedMsgs -> {
                                        if (updatedMsgs.isEmpty()) return Mono.empty();

                                        Map<String, List<String>> senderMsgMap = updatedMsgs.stream()
                                            .collect(Collectors.groupingBy(
                                                com.letschat.mvp_1.Models.MessageTrackHistory::getSenderId,
                                                Collectors.mapping(com.letschat.mvp_1.Models.MessageTrackHistory::getMessageId, Collectors.toList())
                                            ));

                                        return Flux.fromIterable(senderMsgMap.entrySet())
                                            .flatMap(entry -> {
                                                String senderId = entry.getKey();
                                                List<String> msgIds = entry.getValue();

                                                if (usersink.containsKey(senderId)) {
                                                    ACKMessageDTO ack = new ACKMessageDTO(chatId, msgIds, "read", now);
                                                    ack.setread_users(java.util.Collections.singletonList(Userid));
                                                    Sinks.Many<String> ackSink = usersink.get(senderId);
                                                    return sendToSinkAsync(ackSink, ack);
                                                }
                                                return Mono.empty();
                                            })
                                            .then();
                                    });
                            }
                            case "check-out": {
                                System.out.println("check out " + chatId);
                                userstatus.compute(Userid, (key, chatmap) -> {
                                    if (chatmap == null) chatmap = new ConcurrentHashMap<>();
                                    chatmap.put(chatId, "in-active");
                                    return chatmap;
                                });
                                return Mono.empty();
                            }
                            case "media-opened": {
                                LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
                                String msgid = data.getmsgid();
                                System.out.println("media-opened " + msgid + " in " + chatId + " by " + Userid);
                                return messageTrackHistoryRepo.updateonmediaopened(Userid, msgid, now)
                                    .collectList()
                                    .flatMap(openedMsgs -> {
                                        if (openedMsgs.isEmpty()) return Mono.empty();

                                        Map<String, List<String>> senderMsgMap = openedMsgs.stream()
                                            .collect(Collectors.groupingBy(
                                                com.letschat.mvp_1.Models.MessageTrackHistory::getSenderId,
                                                Collectors.mapping(com.letschat.mvp_1.Models.MessageTrackHistory::getMessageId, Collectors.toList())
                                            ));

                                        return Flux.fromIterable(senderMsgMap.entrySet())
                                            .flatMap(entry -> {
                                                String senderId = entry.getKey();
                                                List<String> msgIds = entry.getValue();

                                                if (usersink.containsKey(senderId)) {
                                                    ACKMessageDTO ack = new ACKMessageDTO(chatId, msgIds, "media-opened", now);
                                                    ack.setread_users(java.util.Collections.singletonList(Userid));
                                                    Sinks.Many<String> ackSink = usersink.get(senderId);
                                                    return sendToSinkAsync(ackSink, ack);
                                                }
                                                return Mono.empty();
                                            })
                                            .then();
                                    });
                            }
                            case "delete-for-me": {
                                String msgid = data.getmsgid();
                                System.out.println("delete " + msgid + " from " + chatId + "me");
                                return messageTrackHistoryRepo.deletemessage(Userid, msgid).then();
                            }
                            case "delete-for-eone": {
                                String msgid = data.getmsgid();
                                System.out.println("delete " + msgid + " from " + chatId + " eone");
                                return messageInfoRepo.deleteforeone(Userid, msgid)
                                    .flatMapMany(u -> messageTrackHistoryRepo.getmsgid(msgid)
                                        .flatMap(msg -> {
                                            System.out.println(msg.getRecieverId());
                                            if (usersink.containsKey(msg.getRecieverId())) {
                                                ACKMessageDTO ack = new ACKMessageDTO();
                                                ack.setchatid(chatId);
                                                ack.setmsgid(msgid);
                                                ack.setstatus("deleted" + Userid);
                                                Sinks.Many<String> ack_sink = usersink.get(msg.getRecieverId());
                                                return Mono.fromCallable(() -> objectMapper.writeValueAsString(ack))
                                                    .flatMap(ackjson -> {
                                                        ack_sink.tryEmitNext(ackjson);
                                                        return Mono.empty();
                                                    })
                                                    .onErrorResume(e -> {
                                                        System.out.println(e);
                                                        return Mono.empty();
                                                    });
                                            }
                                            return Mono.empty();
                                        })
                                        .switchIfEmpty(
                                            Mono.fromRunnable(() -> System.out.println("Inner Message history not found for msgid: " + msgid))
                                        ).then()
                                    ).switchIfEmpty(
                                        Mono.fromRunnable(() -> System.out.println("Outer Message history not found for msgid: " + msgid))
                                    ).then();
                            }
                            case "revive-for-me": {
                                String msgid = data.getmsgid();
                                System.out.println("revive " + msgid + " from " + chatId + "me");
                                return messageTrackHistoryRepo.reviveforme(Userid, msgid).then();
                            }
                            case "revive-for-eone": {
                                String msgid = data.getmsgid();
                                System.out.println("revive " + msgid + " from " + chatId + " eone");
                                return messageInfoRepo.reviveforeone(Userid, msgid)
                                    .flatMapMany(u -> messageTrackHistoryRepo.getmsgid(msgid))
                                    .flatMap(msg -> {
                                        System.out.println(msg.getRecieverId());
                                        if (usersink.containsKey(msg.getRecieverId())) {
                                            ACKMessageDTO ack = new ACKMessageDTO();
                                            ack.setchatid(chatId);
                                            ack.setmsgid(msgid);
                                            ack.setstatus("revived");
                                            Sinks.Many<String> ack_sink = usersink.get(msg.getRecieverId());
                                            return Mono.fromCallable(() -> objectMapper.writeValueAsString(ack))
                                                .flatMap(ackjson -> {
                                                    ack_sink.tryEmitNext(ackjson);
                                                    return Mono.empty();
                                                })
                                                .onErrorResume(e -> {
                                                    System.out.println(e);
                                                    return Mono.empty();
                                                });
                                        }
                                        return Mono.empty();
                                    }).then();
                            }
                            default:
                                return Mono.empty();
                        }
                    });
            })
            .onErrorResume(e -> {
                System.out.println(e);
                return Mono.empty();
            });
    }

    public Mono<Void> onconnect(String Userid){
        System.out.println("onconnect");
        Sinks.Many<String> sender_sink = usersink.get(Userid);
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

        Mono<Void> sendPendingMono = messageInfoRepo.loadpending(Userid)
            .doOnNext(msg0 -> {
                try {
                    System.out.println("sending pending");
                    if (sender_sink != null) {
                        sender_sink.tryEmitNext(objectMapper.writeValueAsString(msg0));
                    }
                } catch (JsonProcessingException e) {
                }
            })
            .then();

        Mono<Void> updateDeliveredMono = messageTrackHistoryRepo.updatestatusonconnect(Userid, now)
            .collectList()
            .flatMap(deliveredMsgs -> {
                if (deliveredMsgs.isEmpty()) return Mono.empty();

                Map<String, List<String>> senderMsgMap = deliveredMsgs.stream()
                    .collect(Collectors.groupingBy(
                        com.letschat.mvp_1.Models.MessageTrackHistory::getSenderId,
                        Collectors.mapping(com.letschat.mvp_1.Models.MessageTrackHistory::getMessageId, Collectors.toList())
                    ));

                return Flux.fromIterable(senderMsgMap.entrySet())
                    .flatMap(entry -> {
                        String senderId = entry.getKey();
                        List<String> msgIds = entry.getValue();

                        if (usersink.containsKey(senderId)) {
                            return messageTrackHistoryRepo.getchatidfrommsgid(msgIds.get(0))
                                .flatMap(chatid -> {
                                    ACKMessageDTO ack = new ACKMessageDTO(chatid, msgIds, "delivered", now);
                                    Sinks.Many<String> ackSink = usersink.get(senderId);
                                    return sendToSinkAsync(ackSink, ack);
                                });
                        }
                        return Mono.empty();
                    })
                    .then();
            });

        return sendPendingMono.then(updateDeliveredMono);
    }
    public Mono<Void> announce(String Chatid, String purpose, String username, String admin, String adminid) {
        return announce(Chatid, purpose, username, admin, adminid, 0);
    }

    public Mono<Void> announce(String Chatid, String purpose, String username, String admin, String adminid, Integer spaceId) {
        String msgId = "Msg-" + UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        System.out.println(adminid);
        String sender = "server";
        String contents;
        String youcontent;
        int targetSpace = (spaceId != null) ? spaceId : 0;
        String safeAdmin = (admin != null && !admin.trim().isEmpty()) ? admin.trim() : "Admin";
        String safeUsername = (username != null) ? username.trim() : "";
        String safeAdminId = (adminid != null) ? adminid.trim() : "";

        if ("added".equals(purpose)) {
            contents = safeAdmin + " added " + safeUsername;
            youcontent = "You added " + safeUsername;
        } else if ("promote".equals(purpose)) {
            contents = safeUsername + " is now an admin by " + safeAdmin;
            youcontent = safeUsername + " is now an admin by You";
        } else if ("removed".equals(purpose)) {
            contents = safeAdmin + " removed " + safeUsername;
            youcontent = "You removed " + safeUsername;
        } else if ("created".equals(purpose)) {
            contents = safeAdmin + " created " + safeUsername;
            youcontent = "You created " + safeUsername;
        } else if ("space_created".equals(purpose)) {
            contents = safeAdmin + " created space '" + safeUsername + "'";
            youcontent = "You created space '" + safeUsername + "'";
        } else if ("group_name".equals(purpose)) {
            contents = safeAdmin + " changed the group name to '" + safeUsername + "'";
            youcontent = "You changed the group name to '" + safeUsername + "'";
        } else if ("group_profile".equals(purpose)) {
            contents = safeAdmin + " changed the group icon";
            youcontent = "You changed the group icon";
        } else if ("group_description".equals(purpose)) {
            contents = safeAdmin + " changed the group description";
            youcontent = "You changed the group description";
        } else if ("change".equals(purpose) || "username_change".equals(purpose)) {
            if (!safeUsername.isEmpty() && !safeUsername.equals(safeAdmin)) {
                contents = safeUsername + " changed their name to " + safeAdmin;
                youcontent = "You changed your name to " + safeAdmin;
            } else {
                contents = safeAdmin + " changed their name";
                youcontent = "You changed your name to " + safeAdmin;
            }
        } else {
            contents = safeUsername + " left";
            youcontent = "You left";
        }
        String Userid = safeAdminId;
        System.out.println(contents);
        String messageType = ("group_name".equals(purpose) || "change".equals(purpose) || "username_change".equals(purpose))
                ? "banner-pin"
                : "banner";

        AtomicReference<String> status = new AtomicReference<>("pending");
        return messageInfoRepo.insert(msgId, Chatid, Userid, messageType, contents + "/" + youcontent, null, null, now, targetSpace)
        .flatMap(info -> {
                    return getUserIds(Chatid, null)
                    .flatMap(userids -> {
                        System.out.println(userids);
                        Sinks.Many<String> reciever_sink = usersink.get(userids);
                        
                        SendingMessageDTO msg = new SendingMessageDTO();
                        msg.setmsgid(msgId);
                        msg.setchatid(Chatid);
                        msg.settype(messageType);
                        if (safeAdminId != null && safeAdminId.equals(userids)) {
                            msg.setcontent(youcontent);
                        } else {
                            msg.setcontent(contents);
                        }                       
                        msg.setsendername(sender);
                        msg.settimestamp(now);
                        msg.setspaceid(targetSpace);
                        System.out.println(msg.getcontent());
                        try {
                            String sendjson=objectMapper.writeValueAsString(msg);
                            
                            if(reciever_sink!=null){
                                
                                reciever_sink.tryEmitNext(sendjson);
                              //status="delivered";
                              status.set("delivered");
                            //   if(userstatus.get(userids)==null){
                            //     userstatus.compute(Userid, (key,chatmap)->{
                            //         if(chatmap==null){
                            //             chatmap=new ConcurrentHashMap<>();
                            //         }
                            //         chatmap.put(Chatid,"in-active");
                            //         return chatmap;
                            //     });
                            //   }
                                userstatus.computeIfAbsent(userids, k -> new ConcurrentHashMap<>())
                                .putIfAbsent(Chatid, "in-active");

                                if(userstatus.get(userids).get(Chatid).equals("active")){
                                    
                                    //status="read";
                                    status.set("read");
                                }
                                //deltime.set(LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")));
                            }
                            else{
                                System.out.println("User is offline");
                                
                            }
                            
                        } catch (JsonProcessingException e) {
                            System.out.println("in send"+e);
                        }
                        return messageTrackHistoryRepo.insert(msgId, Userid, userids, status.get(), LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")))
                        .then(Mono.just("inserted"));
                    }).then();
        }).then();

    }

    public Mono<Void> broadcastBannerPin(String chatId, String msgId, String senderId, String othersContent, String youContent, String messageType, String eventId, java.time.LocalDateTime now) {
        return getUserIds(chatId, null)
            .collectList()
            .flatMap(memberList -> {
                java.util.Set<String> allMembers = new java.util.HashSet<>(memberList);
                if (senderId != null && !senderId.isEmpty()) {
                    allMembers.add(senderId);
                }
                return Flux.fromIterable(allMembers)
                    .flatMap(targetUserId -> {
                        AtomicReference<String> status = new AtomicReference<>(targetUserId.equals(senderId) ? "read" : "pending");
                        reactor.core.publisher.Sinks.Many<String> receiverSink = usersink.get(targetUserId);
                        SendingMessageDTO msg = new SendingMessageDTO();
                        msg.setmsgid(msgId);
                        msg.setchatid(chatId);
                        msg.settype(messageType);
                        msg.setcontent(senderId != null && senderId.equals(targetUserId) ? youContent : othersContent);
                        msg.setsendername("server");
                        msg.setsenderid(senderId);
                        msg.settimestamp(now);
                        msg.setspaceid(0);
                        msg.setrepliedto(eventId);
                        try {
                            String sendJson = objectMapper.writeValueAsString(msg);
                            if (receiverSink != null) {
                                receiverSink.tryEmitNext(sendJson);
                                if (!targetUserId.equals(senderId)) {
                                    status.set("delivered");
                                    userstatus.computeIfAbsent(targetUserId, k -> new ConcurrentHashMap<>())
                                        .putIfAbsent(chatId, "in-active");
                                    if ("active".equals(userstatus.get(targetUserId).get(chatId))) {
                                        status.set("read");
                                    }
                                }
                            } else {
                                System.out.println("User is offline for banner pin: " + targetUserId);
                            }
                        } catch (Exception e) {
                            System.err.println("broadcastBannerPin error: " + e.getMessage());
                        }
                        return messageTrackHistoryRepo.insert(msgId, senderId, targetUserId, status.get(), now)
                            .then(Mono.just("inserted"));
                    })
                    .then();
            });
    }

    public Mono<Void> send(ScheduleMessage msg) {

    String msgId = "Msg-" + UUID.randomUUID();
        System.out.println("sedning scheduled");
    return messageInfoRepo.insert(
            msgId,
            msg.getChatId(),
            msg.getSenderId(),
            msg.getMessageType(),
            msg.getMessage(),
            null,
            null,
            msg.getTime(),
            msg.getSpaceId()
    )
    .then(getusername(msg.getChatId(), msg.getSenderId()))
    .flatMapMany(username ->
        getUserIds(msg.getChatId(), "null")
        .flatMap(uid -> {

            String status = "pending";
            LocalDateTime delTime = null;

            Sinks.Many<String> receiverSink = usersink.get(uid);

            SendingMessageDTO sendmsg = new SendingMessageDTO();
            sendmsg.setmsgid(msgId);
            sendmsg.setchatid(msg.getChatId());
            if(!uid.equals(msg.getSenderId())){
                sendmsg.setsendername(username);
            }
            sendmsg.setsenderid(msg.getSenderId());
            sendmsg.settype(msg.getMessageType());
            sendmsg.setcontent(msg.getMessage());
            sendmsg.settimestamp(msg.getTime());
            sendmsg.setspaceid(msg.getSpaceId());

            String sendjson;

            try {
                sendjson = objectMapper.writeValueAsString(sendmsg);
            } catch (Exception e) {
                return Mono.error(e);
            }
            boolean isReceiverActive = receiverSink != null && "active".equals(
                userstatus.getOrDefault(uid, new ConcurrentHashMap<>()).get(msg.getChatId())
            );

            if (!uid.equals(msg.getSenderId()) && !isReceiverActive) {
                ReceivingMessageDTO schedData = new ReceivingMessageDTO();
                schedData.setcontent(msg.getMessage());
                schedData.settype(msg.getMessageType());
                schedData.setspaceid(msg.getSpaceId());
                schedData.settimestamp(msg.getTime());
                fireNotification(uid, msg.getChatId(), msg.getSenderId(), username, schedData, "group", msgId);
            }                                
         
            if (receiverSink != null) {

                receiverSink.tryEmitNext(sendjson);

                status = "delivered";
                delTime = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

                userstatus
                        .computeIfAbsent(uid, k -> new ConcurrentHashMap<>())
                        .putIfAbsent(msg.getChatId(), "in-active");

                if ("active".equals(
                        userstatus
                                .getOrDefault(uid, new ConcurrentHashMap<>())
                                .get(msg.getChatId()))) {

                    status = "read";
                }
                if(!uid.equals(msg.getSenderId())){
                    ACKMessageDTO ack=new ACKMessageDTO();
                    ack.setchatid(msg.getChatId());
                    ack.setmsgid(msgId);
                }

            }else{
              //  String payload=msg.getSpaceId()+"/%20/"+msg.getMessageType()+"/%20/"+username+"/%20/"+msg.getMessage();
              //  notificationService.notifyUser(uid,msg.getChatId(),username,payload,null).subscribe();                                
            }

            return messageTrackHistoryRepo.insert(
                    msgId,
                    msg.getSenderId(),
                    uid,
                    status,
                    delTime
            ).then();   // ignore returned entity
        })
    )
    .then();
}

    public Mono<Void> broadcastEventSync(String chatId, String senderId, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            return getUserIds(chatId, senderId)
                .doOnNext(memberId -> {
                    Sinks.Many<String> sink = usersink.get(memberId);
                    if (sink != null) {
                        sink.tryEmitNext(json);
                    }
                })
                .then();
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    public Mono<Void> broadcastToChat(String chatId, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            return getUserIds(chatId, "")
                .doOnNext(memberId -> {
                    Sinks.Many<String> sink = usersink.get(memberId);
                    if (sink != null) {
                        sink.tryEmitNext(json);
                    }
                })
                .then();
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    public void sendToUser(String targetUserId, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            Sinks.Many<String> sink = usersink.get(targetUserId);
            if (sink != null) {
                sink.tryEmitNext(json);
            }
        } catch (Exception e) {}
    }
}
