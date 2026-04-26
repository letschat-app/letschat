package com.letschat.mvp_1.WebSocket;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
    public MyWebSocketHandler(UserChatInfoRepo userChatInfoRepo,UserInfoRepo userInfoRepo,ObjectMapper objectMapper,MessageInfoRepo messageInfoRepo,MessageTrackHistoryRepo messageTrackHistoryRepo,NotificationService notificationService,AdminService adminService){
        this.userChatInfoRepo=userChatInfoRepo;
        this.userInfoRepo=userInfoRepo;
        this.objectMapper=objectMapper;
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.messageTrackHistoryRepo=messageTrackHistoryRepo;
        this.messageInfoRepo=messageInfoRepo;
        this.notificationService=notificationService;
        this.adminService=adminService;
    }
    private final ConcurrentHashMap<String,ConcurrentHashMap<String,String>> userstatus=new ConcurrentHashMap<>();
    private final Map<String,String>userid=new ConcurrentHashMap<>();
    private final Map<String,Sinks.Many<String>>usersink=new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String,ConcurrentHashMap<String,String>> username=new ConcurrentHashMap<>();
    //chatid<userid,username>
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<String>> userchats = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> chatypes=new ConcurrentHashMap<>();
  //  private final Map<String, Set<String>> tokenCache = new ConcurrentHashMap<>();
    @Override
    public Mono<Void> handle(WebSocketSession session){
            String sessionid = session.getId();

            Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

            String qry = session.getHandshakeInfo().getUri().getQuery();
            String id = null;
            if (qry != null && qry.contains("userid=")) {
                id = qry.split("userid=")[1].split("&")[0];
            }

            if (id == null) {
                return session.close(); // avoid dummy user
            }

            System.out.println("connected:" + id);

            usersink.compute(id, (key, oldSink) -> {
                if (oldSink != null) {
                System.out.println("removing old");
                oldSink.tryEmitComplete();
            }
            return sink;
        });
        adminService.onUserConnected(id);
        userid.put(sessionid, id);

        System.out.println(userid.size() + "," + usersink.size());

        Mono<Void> connect = onconnect(id).then();

        Mono<Void> recieve = session.receive()
        .flatMap(msg -> onrecieve(sessionid, msg.getPayloadAsText()))
        .doOnError(err -> System.out.println("error:" + err.getMessage()))
        .doFinally(status -> {
            String uid = userid.remove(sessionid);
            System.out.println(userid.size());
            adminService.onUserDisconnected(uid);
            // FIX 4: remove only if it's same sink
            usersink.computeIfPresent(uid, (key, currentSink) -> {
                if (currentSink == sink) {
                    currentSink.tryEmitComplete();
                    System.out.println("closed");
                    return null;
                }
                return currentSink;
            });

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
        return connect.then(Mono.when(recieve,send));
    }
    public Mono<String> getusername(String chatId,String userId){
        System.out.println(chatId+userId);
        return Mono.defer(() -> {
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

    // username.computeIfAbsent(chatId, k -> new ConcurrentHashMap<>())
    //         .put(userId, newName);
    Map<String, String> chatMap = username.computeIfAbsent(chatId, k -> new ConcurrentHashMap<>());
    String oldName = chatMap.put(userId, newName);
    this.announce(chatId, "change", oldName, newName, userId);
}

public void removeUsernameCache(String chatId, String userId) {

    ConcurrentHashMap<String, String> map = username.get(chatId);

    if (map != null) {
        map.remove(userId);
    }
}
public Flux<String> getUserIds(String chatId, String userId) {

    return Flux.defer(() -> {

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
        return Mono.defer(()->{
            String type=chatypes.get(chatid);
            if(type != null){
                return Mono.just(type);
            }

            return userChatInfoRepo.findTypeByChatId(chatid)
            .doOnNext(type1->chatypes.putIfAbsent(chatid,type1));
        });
    }



public Mono<Void> onrecieve(String sessionId, String json) {

    return Mono.fromCallable(() -> objectMapper.readValue(json, ReceivingMessageDTO.class))
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(data -> {

            if (data.getchatid() == null) {
                return Mono.error(new RuntimeException("ChatId is null"));
            }

            String msgId = "Msg-" + UUID.randomUUID();
            String chatId = data.getchatid();
            String userId = this.userid.get(sessionId);

            if (userId == null) {
                return Mono.error(new RuntimeException("Invalid session"));
            }

            Sinks.Many<String> senderSink = usersink.get(userId);

            // ── INDICATOR ────────────────────────────────────────────────────
            if ("indicator".equals(data.gettype())) {
                return getusername(chatId, userId)
                    .flatMap(username ->
                        getUserIds(chatId, userId)
                            .flatMap(receiverId -> {
                                Sinks.Many<String> receiverSink = usersink.get(receiverId);
                                SendingMessageDTO msg = buildMsg(null, chatId, data, username, userId);
                                return sendToSinkAsync(receiverSink, msg);
                            }).then()
                    );
            }

            // ── NORMAL FLOW ──────────────────────────────────────────────────
            return messageInfoRepo.insert(
                    msgId, chatId, userId,
                    data.gettype(), data.getcontent(),
                    data.getrepliedto(), data.getforwardedfrom(),
                    data.gettimestamp(), data.getspaceid()
                )
                .flatMap(info -> getType(chatId)
                    .flatMap(type -> {

                        // ── CLASSROOM / ROOM ──────────────────────────────────
                        if ("classroom".equals(type) || "room".equals(type)) {

                            return sendAckAsync(senderSink, chatId, data.gettempmsgid(), msgId, "read")
                                .then(getusername(chatId, userId))
                                .flatMap(username ->
                                    getUserIds(chatId, userId)
                                        .flatMap(receiverId -> {
                                            System.out.println("sednerid:"+userId+"sendername:"+username);
                                            Sinks.Many<String> receiverSink = usersink.get(receiverId);
                                            SendingMessageDTO msg = buildMsg(msgId, chatId, data, username, userId);
                                            fireNotification(receiverId, chatId, username, data, type);
                                            if (receiverSink != null) {
                                               // fireNotification(receiverId, chatId, username, data, type);
                                                return sendToSinkAsync(receiverSink, msg);//.doOnNext(fireNotification(receiverId, chatId, username, data, type));
                                            }
                                            return Mono.empty();
                                        }).then()
                                );
                        }

                        // ── NORMAL CHAT ───────────────────────────────────────
                        return getusername(chatId, userId)
                            .flatMap(username ->
                                getUserIds(chatId, userId)
                                    .flatMap(receiverId -> {
                                        System.out.println("sednerid:"+userId+"sendername:"+username);
                                        Sinks.Many<String> receiverSink = usersink.get(receiverId);
                                        SendingMessageDTO msg = buildMsg(msgId, chatId, data, username, userId);
                                        fireNotification(receiverId, chatId, username, data, type);
                                        if (receiverSink != null) {
                                            userstatus.computeIfAbsent(receiverId, k -> new ConcurrentHashMap<>())
                                                      .putIfAbsent(chatId, "in-active");

                                            boolean isActive = "active".equals(
                                                userstatus.getOrDefault(receiverId, new ConcurrentHashMap<>()).get(chatId)
                                            );

                                            String status = isActive ? "read" : "delivered";
                                            LocalDateTime deliveredTime = LocalDateTime.now();

                                            return sendToSinkAsync(receiverSink, msg)
                                                .then(messageTrackHistoryRepo.insert(
                                                    msgId, userId, receiverId, status, deliveredTime
                                                ))
                                                .thenReturn(status);

                                        } else {
                                            // fireNotification(receiverId, chatId, username, data, type);

                                            return messageTrackHistoryRepo.insert(
                                                    msgId, userId, receiverId, "pending", null
                                                )
                                                .thenReturn("pending");
                                        }
                                    })
                                    .reduce("read", (acc, curr) -> {
                                        if ("pending".equals(acc) || "pending".equals(curr)) return "pending";
                                        if ("delivered".equals(acc) || "delivered".equals(curr)) return "delivered";
                                        return "read";
                                    })
                                    .flatMap(finalStatus ->
                                        sendAckAsync(senderSink, chatId, data.gettempmsgid(), msgId, finalStatus)
                                    )
                            );

                    }).then()
                ).then()
                .doOnSuccess(v -> {adminService.onMessageSent();adminService.onUserActivity(userId);});
        })
        .onErrorResume(e -> {
            System.out.println("load msg calling");
            return loadmsg(sessionId, json);
        }).then();
}

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
        .subscribeOn(Schedulers.boundedElastic())
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

private void fireNotification(String receiverId, String chatId,
                               String username, ReceivingMessageDTO data, String type) {
    String payload = data.getspaceid() + "/%20/" + data.gettype()
                   + "/%20/" + username + "/%20/" + data.getcontent();
    notificationService.notifyUser(receiverId, chatId, username, payload, type)
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> {
            System.out.println("FCM error: " + e);
            return Mono.empty();
        })
        .subscribe();
}

    @SuppressWarnings("ConvertToStringSwitch")
    public Mono<Void> loadmsg(String sessionid,String json){
        System.out.println("load msg called");
        String Userid=userid.get(sessionid);
        Sinks.Many<String> sender_sink=usersink.get(Userid);
       // String Chatid;
        return Mono.fromCallable(()->objectMapper.readValue(json,ChatUpdateDTO.class))
            .flatMap(data->{
                return getType(data.getuserchatid())
                //return userChatInfoRepo.findTypeByChatId(data.getuserchatid())
                .flatMap(type->{
                System.out.println(data.getpurpose());
                System.out.println(data.getuserchatid());
                if("classroom".equals(type) || "room".equals(type)){
                    if(data.getpurpose().equals("load")){
                        return messageInfoRepo.loadforroom(data.getuserchatid())
                        .flatMap(msg->{
                            notificationService.clear(Userid, data.getuserchatid());
                            return Mono.fromCallable(()->objectMapper.writeValueAsString(msg))
                            .flatMap(msgjson->{
                                sender_sink.tryEmitNext(msgjson);
                                return Mono.empty();
                            });
                        }).then();                       
                    }
                }
                System.out.print(json);
                if(data.getpurpose().equals("load")){
                    
                    userstatus.compute(Userid, (key,chatmap)->{
                        if(chatmap==null){
                            chatmap=new ConcurrentHashMap<>();
                        }
                        chatmap.put(data.getuserchatid(),"active");
                        return chatmap;
                    });
                    //Chatid=data.getuserchatid();
                    return messageInfoRepo.load(Userid, data.getuserchatid())
                    .flatMap(msg->{
                        System.out.println(msg);
                        return Mono.fromCallable(()->objectMapper.writeValueAsString(msg))
                        .flatMap(msgjson->{
                            //Sinks.Many<String> sender_sink=usersink.get(Userid);
                            sender_sink.tryEmitNext(msgjson);
                            // return messageTrackHistoryRepo.updatestatusonchat(msg.getmsgid(), Userid,LocalDateTime.now())
                            // .flatMap(updmsg->{
                            //     if(usersink.containsKey(updmsg.getSenderId())){
                            //         ACKMessageDTO ack=new ACKMessageDTO();
                            //         ack.setchatid(msg.getchatid());
                            //         ack.setmsgid(msg.getmsgid());
                            //         ack.setstatus("read");
                            //         Sinks.Many<String> ack_sink=usersink.get(updmsg.getSenderId());
                            //         return Mono.fromCallable(()->objectMapper.writeValueAsString(ack))
                            //         .flatMap(ackjson->{
                            //             ack_sink.tryEmitNext(ackjson);
                            //             return Mono.empty();
                            //         })
                            //         .onErrorResume(e->{
                            //             System.out.println(e);
                            //             return Mono.empty();
                            //         });
                            //     }
                            //     else{
                            //         return Mono.empty();
                            //     }
                            // });
                            notificationService.clear(Userid, data.getuserchatid());
                            return Mono.empty();
                        })
                        .onErrorResume(e->{
                            System.out.println(e);
                            return Mono.empty();
                        });
                    }).then();
                }
                else if(data.getpurpose().equals("check-in")){
                    LocalDateTime now=LocalDateTime.now();
                    String chatid=data.getuserchatid();
                    System.out.println("check in "+chatid);
                    userstatus.compute(Userid, (key,chatmap)->{
                        if(chatmap==null){
                            chatmap=new ConcurrentHashMap<>();
                        }
                        chatmap.put(chatid,"active");
                        return chatmap;
                    });
                    Integer space=Integer.parseInt(data.getmsgid());
                    return messageTrackHistoryRepo.updateoncheckin(Userid,chatid,now,space)
                    .flatMap(msg->{
                        if(usersink.containsKey(msg.getSenderId())){
                            ACKMessageDTO ack=new ACKMessageDTO();
                            ack.setchatid(chatid);
                            ack.setmsgid(msg.getMessageId());
                            ack.setstatus("read");
                            Sinks.Many<String> ack_sink=usersink.get(msg.getSenderId());
                            return Mono.fromCallable(()->objectMapper.writeValueAsString(ack))
                            .flatMap(ackjson->{
                                //ack_sink.tryEmitNext(ackjson);
                                try {
                                    ack_sink.emitNext(objectMapper.writeValueAsString(ackjson),
                                    (signalType, emitResult) -> emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED);
                                    notificationService.clear(Userid, data.getuserchatid());
                                } catch (JsonProcessingException e) {  

                                }
                                return Mono.empty();
                            })
                            .onErrorResume(e->{
                            System.out.println(e);
                            return Mono.empty();
                            });
                        }
                        return Mono.empty();
                    }).then();
                }
                else if(data.getpurpose().equals("check-out")){                    
                    String chatid=data.getuserchatid();
                    System.out.println("check out "+chatid);
                    userstatus.compute(Userid, (key,chatmap)->{
                        if(chatmap==null){
                            chatmap=new ConcurrentHashMap<>();
                        }
                        chatmap.put(chatid,"in-active");
                        return chatmap;
                    });
                    return Mono.empty();
                }
                else if(data.getpurpose().equals("delete-for-me")){
                    String chatid=data.getuserchatid();
                    String msgid=data.getmsgid();
                    System.out.println("delete "+msgid+" from "+chatid+"me");
                    return messageTrackHistoryRepo.deletemessage(Userid, msgid).then();
                    
                }
                else if(data.getpurpose().equals("delete-for-eone")){
                    String chatid=data.getuserchatid();
                    String msgid=data.getmsgid();
                    System.out.println("delete "+msgid+" from "+chatid+" eone");
                    return messageInfoRepo.deleteforeone(Userid, msgid)
                    .flatMap(u->{
                        return messageTrackHistoryRepo.getmsgid(msgid)
                    
                    .flatMap(msg->{
                        System.out.println(msg.getRecieverId());
                        if(usersink.containsKey(msg.getRecieverId())){
                            ACKMessageDTO ack=new ACKMessageDTO();
                            ack.setchatid(chatid);
                            ack.setmsgid(msgid);
                            ack.setstatus("deleted"+Userid);
                            Sinks.Many<String> ack_sink=usersink.get(msg.getRecieverId());
                            return Mono.fromCallable(()->objectMapper.writeValueAsString(ack))
                            .flatMap(ackjson->{
                                ack_sink.tryEmitNext(ackjson);
                                return Mono.empty();
                            })
                            .onErrorResume(e->{
                            System.out.println(e);
                            return Mono.empty();
                            });
                        }
                        return Mono.empty();
                    })
                    .switchIfEmpty(
        // THIS RUNS IF 'msg' WAS NOT FOUND (i.e., the Mono was empty)
        Mono.fromRunnable(() -> System.out.println("Inner Message history not found for msgid: " + msgid))
    ).then();
                    }).switchIfEmpty(
        // THIS RUNS IF 'msg' WAS NOT FOUND (i.e., the Mono was empty)
        Mono.fromRunnable(() -> System.out.println("Outer Message history not found for msgid: " + msgid))
    ).then();
                }
                else if(data.getpurpose().equals("revive-for-me")){
                    String chatid=data.getuserchatid();
                    String msgid=data.getmsgid();
                    System.out.println("revive "+msgid+" from "+chatid+"me");
                    return messageTrackHistoryRepo.reviveforme(Userid, msgid).then();
                }
                else if(data.getpurpose().equals("revive-for-eone")){
                    String chatid=data.getuserchatid();
                    String msgid=data.getmsgid();
                    System.out.println("revive "+msgid+" from "+chatid+" eone");
                    return messageInfoRepo.reviveforeone(Userid, msgid)
                    .flatMapMany(u->messageTrackHistoryRepo.getmsgid(msgid))
                    .flatMap(msg->{
                        System.out.println(msg.getRecieverId());
                        if(usersink.containsKey(msg.getRecieverId())){
                            ACKMessageDTO ack=new ACKMessageDTO();
                            ack.setchatid(chatid);
                            ack.setmsgid(msgid);
                            ack.setstatus("revived");
                            Sinks.Many<String> ack_sink=usersink.get(msg.getRecieverId());
                            return Mono.fromCallable(()->objectMapper.writeValueAsString(ack))
                            .flatMap(ackjson->{
                                ack_sink.tryEmitNext(ackjson);
                                return Mono.empty();
                            })
                            .onErrorResume(e->{
                            System.out.println(e);
                            return Mono.empty();
                            });
                        }
                        return Mono.empty();
                    }).then();
                }
                else{
                    return Mono.empty();
                }
            });});
    }

    public Mono<Void> onconnect(String Userid){
        System.out.println("onconnect");
        Sinks.Many<String> sender_sink=usersink.get(Userid);
        return messageInfoRepo.loadpending(Userid)
        .flatMap(msg0->{
            
            try {
                System.out.println("sending pending");
                sender_sink.tryEmitNext(objectMapper.writeValueAsString(msg0));
            } catch (JsonProcessingException e) {
                // TODO: handle exception
            }
         
        return messageTrackHistoryRepo.updatestatusonconnect(Userid,LocalDateTime.now())
        .flatMap(msg->{
            System.out.println("connect");
            if(usersink.containsKey(msg.getSenderId())){
                return messageTrackHistoryRepo.getchatidfrommsgid(msg.getMessageId())
                .flatMap(chatid->{
                    ACKMessageDTO ack=new ACKMessageDTO();
                    ack.setchatid(chatid);
                    ack.setmsgid(msg.getMessageId());
                    ack.setstatus("delivered");
                    Sinks.Many<String> ack_sink=usersink.get(msg.getSenderId());
                    try {
                        System.out.println("deliver ack");
                        // Sinks.EmitResult result=ack_sink.tryEmitNext(objectMapper.writeValueAsString(ack));
                        // if (result.isFailure()) {
                        //     System.out.println("❌ Emit failed: " + result.name());
                        // }
                        ack_sink.emitNext(objectMapper.writeValueAsString(ack),
                            (signalType, emitResult) -> emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED
                        );

                    } catch (JsonProcessingException e3) {

                    }
                    return Mono.empty();
                }).then();
            }
            return Mono.empty();
        }).then();
    }).then();
    }
    public Mono<Void> announce(String Chatid,String purpose,String username,String admin,String adminid){
        String msgId="Msg-"+UUID.randomUUID().toString();
        LocalDateTime now=LocalDateTime.now();
        System.out.println(adminid);
        String sender="server";
        String contents;
        String youcontent;
        if(purpose=="added"){
            contents=admin+" added "+username;
            youcontent="You added "+username;
        }
        else if(purpose=="promote"){
            contents=username+" is now an admin by "+admin;
            youcontent=username+" is now an admin by You";
        }
        else if(purpose=="removed"){
            contents=admin+" removed "+username;
            youcontent="You removed "+username;
        }
        else if(purpose=="created"){
            contents=admin+ " created "+username;
            youcontent="You created "+username;
        }
        else if(purpose=="change"){
            contents=admin+" change profile to "+username;
            youcontent="You changed your profile to"+username;
        }
        else{
            contents=username +" left";
            youcontent="You left";
        }
        String Userid=adminid;
        System.out.println(contents);
        AtomicReference<String> status=new AtomicReference<>();
       // AtomicReference<LocalDateTime> deltime=new AtomicReference<>();
        status.set("pending");
        return messageInfoRepo.insert(msgId, Chatid, Userid, "banner", contents+"/"+youcontent, null, null, now,0)
        .flatMap(info->{
                    return getUserIds(Chatid,null)
                    .flatMap(userids->{
                        System.out.println(userids);
                        Sinks.Many<String> reciever_sink=usersink.get(userids);
                        
                        SendingMessageDTO msg=new SendingMessageDTO();
                        msg.setmsgid(msgId);
                        msg.setchatid(Chatid);
                        msg.settype("banner");
                        if(adminid.equals(userids)){
                            msg.setcontent(youcontent);
                        }
                        else{
                            msg.setcontent(contents);
                        }                       
                        msg.setsendername(sender);
                        msg.settimestamp(now);
                        msg.setspaceid(0);
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
                                //deltime.set(LocalDateTime.now());
                            }
                            else{
                                System.out.println("User is offline");
                                
                            }
                            
                        } catch (JsonProcessingException e) {
                            System.out.println("in send"+e);
                        }
                        return messageTrackHistoryRepo.insert(msgId, Userid, userids, status.get(), LocalDateTime.now())
                        .then(Mono.just("inserted"));
                    }).then();
        }).then();

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
            String payload=msg.getSpaceId()+"/%20/"+msg.getMessageType()+"/%20/"+username+"/%20/"+msg.getMessage();
            notificationService.notifyUser(uid,msg.getChatId(),username,payload,null).subscribe();                                
         
            if (receiverSink != null) {

                receiverSink.tryEmitNext(sendjson);

                status = "delivered";
                delTime = LocalDateTime.now();

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
}
