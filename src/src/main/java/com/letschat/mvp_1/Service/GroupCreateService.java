package com.letschat.mvp_1.Service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.UserSearchResult;
import com.letschat.mvp_1.Repositories.GroupInfoRepo;
import com.letschat.mvp_1.Repositories.IdTableRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class GroupCreateService {
    private final UserInfoRepo userInfoRepo;
    private final UserChatInfoRepo userChatInfoRepo;
    private final GroupInfoRepo groupInfoRepo;
    private final IdTableRepo idTableRepo;
    public GroupCreateService(IdTableRepo idTableRepo,UserInfoRepo userInfoRepo,UserChatInfoRepo userChatInfoRepo,GroupInfoRepo groupInfoRepo){
        this.userInfoRepo=userInfoRepo;
        this.userChatInfoRepo=userChatInfoRepo;
        this.groupInfoRepo=groupInfoRepo;
        this.idTableRepo=idTableRepo;
    }

    public Mono<String> creategroup(String UserId,String GroupName){
        System.out.println("grouping");
        return userInfoRepo.findByUserId(UserId)
        .flatMap(user->generateChatId()
        .flatMap(chatid->{
            LocalDateTime now=LocalDateTime.now();
            return userChatInfoRepo.insert(chatid, user.getUserId(), GroupName, user.getPrivateName(), now, now,"group","admin")
            .flatMap(chat->{ return generateGroupId()
            .doOnNext(groupid->System.out.println("inside"+chat.getChatId()))
            .flatMap(groupid->{
                return groupInfoRepo.insert(groupid, GroupName, user.getUserId(), chat.getId())
                .thenReturn(chatid);
            });})
            .doOnNext(chat->System.out.println("outside"+chat));
        }))
        .switchIfEmpty(Mono.just("failed"));
    }

    public Mono<String> generateGroupIdId(){
        return groupInfoRepo.findlastuser()
        .doOnNext(u->System.out.println(u))
        .map(chat->{
           // System.out.println("in generate:"+user.getUserId()+user.getAge());
            String id=chat.getGroupId();//AAA000
            System.out.println(id);
            int num =Integer.parseInt(id.replaceAll("[^0-9]", ""));
            return String.format("AAA%03d", num+1);
        })
        .defaultIfEmpty("AAA000");
    }

    public Mono<String> generateChatId0(){
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
    public Mono<String> generateChatId(){
        return idTableRepo.getChatid()
        .flatMap(id->{
            return numberToCode(id);
        });
    }
    public Mono<String> generateGroupId(){
        return idTableRepo.getGroupid()
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

    public Flux<UserSearchResult> getMembers(String chatid){
        return groupInfoRepo.getMembers(chatid)
        .flatMap(user->{
            UserSearchResult User=new UserSearchResult();
            User.setUserId(user.getUserId());
            User.setUserName(user.getUserName());
            User.setRole(user.getRole());
            return Mono.just(User);
        })
        .doOnNext(members->System.out.println(members.getUserName()));
    }
    

}
