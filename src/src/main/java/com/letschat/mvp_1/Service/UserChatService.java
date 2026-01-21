package com.letschat.mvp_1.Service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.UserSearchResult;
import com.letschat.mvp_1.Models.UserChatInfo;
import com.letschat.mvp_1.Models.UserInfo;
import com.letschat.mvp_1.Repositories.IdTableRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;

import reactor.core.publisher.Mono;

@Component
public class UserChatService {
    private final UserInfoRepo userInfoRepo;
    private final UserChatInfoRepo userChatInfoRepo;
    private final IdTableRepo idTableRepo;
    public UserChatService(UserInfoRepo userInfoRepo,UserChatInfoRepo userChatInfoRepo,IdTableRepo idTableRepo){
        this.userInfoRepo=userInfoRepo;
        this.userChatInfoRepo=userChatInfoRepo;
        this.idTableRepo=idTableRepo;
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
                UserChatInfo sendchat=new UserChatInfo();
                sendchat.setChatId(chatid);
                sendchat.setUserId(SenderId);
                sendchat.setChatName(receiveuser.getPublicName());
                sendchat.setUserName(senduser.getPublicName());
                sendchat.setAt(now);
                sendchat.setVisited(now);

                UserChatInfo recievechat=new UserChatInfo();
                recievechat.setChatId(chatid);
                recievechat.setUserId(RecieverId);
                recievechat.setChatName(senduser.getPublicName());
                recievechat.setUserName(receiveuser.getPublicName());
                recievechat.setAt(now);
                recievechat.setVisited(now);

                return userChatInfoRepo.insert(chatid, SenderId, receiveuser.getPublicName(), senduser.getPublicName(), now, now,"private",null)
                .then(userChatInfoRepo.insert(chatid, RecieverId, senduser.getPublicName(), receiveuser.getPublicName(), now, now,"private",null))
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
            return Mono.just(User);
        });
    }
    public Mono<String> updateInfo(UserSearchResult info,String Chatid){
        return userChatInfoRepo.findTypeByChatId(Chatid)
        .flatMap(type->{
            System.out.println(info.getUserName());
            System.out.println(type);
            if(type=="private"){
                return userChatInfoRepo.updateUsername(info.getUserName(),Chatid,info.getUserId())
                .then(userChatInfoRepo.updateChatname(info.getUserName(),Chatid,info.getUserId())).thenReturn("ok");
            }
            else{
                return userChatInfoRepo.updateUsername(info.getUserName(),Chatid,info.getUserId()).thenReturn("ok");
            }
        })
        .switchIfEmpty(Mono.just("failed"));
    }
}
