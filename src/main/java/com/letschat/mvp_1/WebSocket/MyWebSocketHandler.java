package com.letschat.mvp_1.WebSocket;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.letschat.mvp_1.DTOs.ACKMessageDTO;
import com.letschat.mvp_1.DTOs.ChatUpdateDTO;
import com.letschat.mvp_1.DTOs.ReceivingMessageDTO;
import com.letschat.mvp_1.DTOs.SendingMessageDTO;
import com.letschat.mvp_1.Repositories.MessageInfoRepo;
import com.letschat.mvp_1.Repositories.MessageTrackHistoryRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
public class MyWebSocketHandler implements WebSocketHandler{

    private final UserChatInfoRepo userChatInfoRepo;
    private final MessageInfoRepo messageInfoRepo;
    private final MessageTrackHistoryRepo messageTrackHistoryRepo;
    private final UserInfoRepo userInfoRepo;
    private final ObjectMapper objectMapper;
    public MyWebSocketHandler(UserChatInfoRepo userChatInfoRepo,UserInfoRepo userInfoRepo,ObjectMapper objectMapper,MessageInfoRepo messageInfoRepo,MessageTrackHistoryRepo messageTrackHistoryRepo){
        this.userChatInfoRepo=userChatInfoRepo;
        this.userInfoRepo=userInfoRepo;
        this.objectMapper=objectMapper;
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.messageTrackHistoryRepo=messageTrackHistoryRepo;
        this.messageInfoRepo=messageInfoRepo;
    }
    private final ConcurrentHashMap<String,ConcurrentHashMap<String,String>> userstatus=new ConcurrentHashMap<>();
    private final Map<String,String>userid=new ConcurrentHashMap<>();
    private final Map<String,Sinks.Many<String>>usersink=new ConcurrentHashMap<>();
    @Override
    public Mono<Void> handle(WebSocketSession session){
        String sessionid=session.getId();
        Sinks.Many<String> sink=Sinks.many().multicast().onBackpressureBuffer();
        String qry=session.getHandshakeInfo().getUri().getQuery();
        String id="dummy";
        if(qry!=null && qry.contains("userid=")){
            id=qry.split("userid=")[1];
        }System.out.println("connected:"+id);
        Sinks.Many<String> ifold=usersink.get(id);
        if(ifold!=null){
            System.out.println("removing old");
            ifold.tryEmitComplete();
        }
        userid.put(sessionid, id);
        usersink.put(id, sink);
         System.out.println(userid.size()+","+usersink.size());
        Mono<Void> connect=onconnect(id).then();

        Mono<Void> recieve=session.receive()
        .flatMap(msg->onrecieve(sessionid, msg.getPayloadAsText()))
        .doOnError(err->System.out.println("error:"+err.getMessage()))
        .doFinally(status->{
            String uid=userid.remove(sessionid);
            System.out.println(userid.size());
            Sinks.Many<String> old=usersink.remove(uid);          
            if(old!=null){
                old.tryEmitComplete();
                System.out.println("closed");
            }
            System.out.println("removed");
        })
        .then();

        Mono<Void> send=session.send(sink.asFlux()
        .doOnSubscribe(sub->System.out.println("to send "+userid.get(sessionid)))
        .doOnCancel(()->System.out.println("to cancel "+userid.get(sessionid)))
        .doOnNext(msg->System.out.println("sedning to"+userid+msg))
        .map(session::textMessage));
        return connect.then(Mono.when(recieve,send));
    }
    public Mono<Void> onrecieve(String sessionid,String json){

        return Mono.fromCallable(()->objectMapper.readValue(json,ReceivingMessageDTO.class))
        .flatMap(data->{
            if(data.getchatid()==null){
                return Mono.error(new RuntimeException());
            }
            String msgId="Msg-"+UUID.randomUUID().toString();
            AtomicReference<String> status=new AtomicReference<>();
            AtomicReference<LocalDateTime> deltime=new AtomicReference<>();
            System.out.println("onrecieve");
           // ReceivingMessageDTO data=objectMapper.readValue(json, ReceivingMessageDTO.class);
            String ChatId=data.getchatid();
            String Userid=userid.get(sessionid);
            Sinks.Many<String> sender_sink=usersink.get(Userid);
            System.out.println(ChatId+"="+Userid+":"+data.gettimestamp());
            return messageInfoRepo.insert(msgId, ChatId, Userid, data.gettype(), data.getcontent(), data.getrepliedto(), data.getforwardedfrom(), data.gettimestamp(),data.getspaceid())
            .flatMap(info->{
                return userInfoRepo.findName(Userid,ChatId)
                .doOnNext(username->System.out.println(username))
                .flatMap(username->{
                    return userChatInfoRepo.findUserIds(ChatId, Userid)
                    .flatMap(userids->{
                        System.out.println(userids);
                        Sinks.Many<String> reciever_sink=usersink.get(userids);
                        status.set("pending");
                        deltime.set(null);
                        SendingMessageDTO msg=new SendingMessageDTO();
                        msg.setmsgid(msgId);
                        msg.setchatid(ChatId);
                        msg.settype(data.gettype());
                        msg.setcontent(data.getcontent());
                        msg.setsendername(username);
                        msg.settimestamp(data.gettimestamp());
                        msg.setrepliedto(data.getrepliedto());
                        msg.setforwardedfrom(data.getforwardedfrom());
                        msg.setspaceid(data.getspaceid());
                        System.out.println(msg.getcontent());
                        try {
                            String sendjson=objectMapper.writeValueAsString(msg);
                            ACKMessageDTO ack=new ACKMessageDTO();
                            ack.setchatid(ChatId);
                            ack.settempmsgid(data.gettempmsgid());
                            ack.setmsgid(msgId);
                            if(reciever_sink!=null){
                                
                                reciever_sink.tryEmitNext(sendjson);
                                ack.setstatus("delivered");
                                status.set("delivered");
                                userstatus.computeIfAbsent(userids, k -> new ConcurrentHashMap<>())
                                .putIfAbsent(ChatId, "in-active");
                                if(userstatus.get(userids).get(ChatId).equals("active")){
                                    ack.setstatus("read");
                                    status.set("read");
                                }
                                deltime.set(LocalDateTime.now());
                            }
                            else{
                                System.out.println("User is offline");
                                ack.setstatus("pending");
                            }
                            try {
                                String ackjson=objectMapper.writeValueAsString(ack);
                                sender_sink.tryEmitNext(ackjson);
                            } catch (JsonProcessingException e) {
                                System.out.println("in ack"+e);
                            }
                        } catch (JsonProcessingException e) {
                            System.out.println("in send"+e);
                        }
                        return messageTrackHistoryRepo.insert(msgId, Userid, userids, status.get(), deltime.get())
                        .then(Mono.just("inserted"));
                    }).then();
                }).then();
            }).then();
        })
        .onErrorResume(e->{
            return loadmsg(sessionid,json);
        }).then();

    }

    @SuppressWarnings("ConvertToStringSwitch")
    public Mono<Void> loadmsg(String sessionid,String json){
        String Userid=userid.get(sessionid);
        Sinks.Many<String> sender_sink=usersink.get(Userid);
        
       // String Chatid;
        return Mono.fromCallable(()->objectMapper.readValue(json,ChatUpdateDTO.class))
            .flatMap(data->{
                System.out.println(data.getpurpose());
                System.out.println(data.getuserchatid());
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
                            return messageTrackHistoryRepo.updatestatusonchat(msg.getmsgid(), Userid,LocalDateTime.now())
                            .flatMap(updmsg->{
                                if(usersink.containsKey(updmsg.getSenderId())){
                                    ACKMessageDTO ack=new ACKMessageDTO();
                                    ack.setchatid(msg.getchatid());
                                    ack.setmsgid(msg.getmsgid());
                                    ack.setstatus("read");
                                    Sinks.Many<String> ack_sink=usersink.get(updmsg.getSenderId());
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
                                else{
                                    return Mono.empty();
                                }
                            });
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
                    return messageTrackHistoryRepo.updateoncheckin(Userid,chatid,now)
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
                                    (signalType, emitResult) -> emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED
                                );
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
            });
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
    public Mono<Void> announce(String Chatid,String purpose,String username){
        String msgId="Msg-"+UUID.randomUUID().toString();
        LocalDateTime now=LocalDateTime.now();
        String sender="server";
        String contents=username+" "+purpose;
        String Userid="AAA003";
        
        AtomicReference<String> status=new AtomicReference<>();
       // AtomicReference<LocalDateTime> deltime=new AtomicReference<>();
        status.set("pending");
        return messageInfoRepo.insert(msgId, Chatid, Userid, "banner", contents, null, null, now,0)
        .flatMap(info->{
                    return userChatInfoRepo.findUserIds(Chatid, Userid)
                    .flatMap(userids->{
                        System.out.println(userids);
                        Sinks.Many<String> reciever_sink=usersink.get(userids);
                        
                        SendingMessageDTO msg=new SendingMessageDTO();
                        msg.setmsgid(msgId);
                        msg.setchatid(Chatid);
                        msg.settype("text");
                        msg.setcontent(contents);
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
                              if(userstatus.get(userids)==null){
                                userstatus.compute(Userid, (key,chatmap)->{
                                    if(chatmap==null){
                                        chatmap=new ConcurrentHashMap<>();
                                    }
                                    chatmap.put(Chatid,"in-active");
                                    return chatmap;
                                });
                              }
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
}
