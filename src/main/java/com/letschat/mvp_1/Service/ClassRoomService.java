package com.letschat.mvp_1.Service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.LoadMessageDTO;
import com.letschat.mvp_1.Models.ClassRoomInfo;
import com.letschat.mvp_1.Repositories.ClassRoomRepo;
import com.letschat.mvp_1.Repositories.IdTableRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;

import reactor.core.publisher.Mono;

@Component
public class ClassRoomService {
    private final UserInfoRepo userInfoRepo;
    private final UserChatInfoRepo userChatInfoRepo;
    private final ClassRoomRepo classRoomRepo;
    private final IdTableRepo idTableRepo;
    public ClassRoomService(IdTableRepo idTableRepo,UserInfoRepo userInfoRepo,UserChatInfoRepo userChatInfoRepo,ClassRoomRepo classRoomRepo){
        this.userInfoRepo=userInfoRepo;
        this.userChatInfoRepo=userChatInfoRepo;
        this.classRoomRepo=classRoomRepo;
        this.idTableRepo=idTableRepo;
    }

    public Mono<String> create(String Userid,String roomname){
       // ClassRoomInfo classRoomInfo = new ClassRoomInfo();
       System.out.println("rooming");
        return userInfoRepo.findByUserId(Userid)
        .flatMap(user -> generateChatId()
        .flatMap(chatid->{
            LocalDateTime now=LocalDateTime.now();
            return userChatInfoRepo.insert(chatid, user.getUserId(), roomname, user.getPrivateName(), now, now,"classroom","faculty")
            .flatMap(chat-> generateRoomId()
            .doOnNext(roomid->System.out.println("inside"+chat.getChatId()))
            .flatMap(roomid->{
                ClassRoomInfo classRoomInfo = new ClassRoomInfo();
                classRoomInfo.setChatId(chat.getId());
                classRoomInfo.setRoomId(roomid);
                classRoomInfo.setFaculty(user.getUserId());
                classRoomInfo.setNoOfStudents(0L);
                classRoomInfo.setRoomName(roomname);
                return classRoomRepo.save(classRoomInfo)
                .thenReturn(chatid);
            })
            ).doOnNext(chat->System.out.println("outside"+chat));
        }))
        .switchIfEmpty(Mono.just("failed"));
    }

    public Mono<String> generateChatId(){
        return idTableRepo.getChatid()
        .flatMap(id->{
            return numberToCode(id);
        });
    }

    public Mono<String> generateRoomId(){
        return idTableRepo.getClassid()
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


    public Mono<Integer> getcount(String chatid,LocalDateTime time){
        return classRoomRepo.getCountofClassRoomMessages(chatid, time);
    }
    public Mono<LoadMessageDTO> getlast(String chatid,LocalDateTime time){
        return classRoomRepo.checknew(chatid, time);
    }
}
