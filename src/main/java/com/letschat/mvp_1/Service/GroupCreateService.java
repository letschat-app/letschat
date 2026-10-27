package com.letschat.mvp_1.Service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.ChatBoxReturnDTO;
import com.letschat.mvp_1.DTOs.UserSearchResult;
import com.letschat.mvp_1.Repositories.GroupInfoRepo;
import com.letschat.mvp_1.Repositories.IdTableRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;
import com.letschat.mvp_1.WebSocket.MyWebSocketHandler;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class GroupCreateService {
    private final UserInfoRepo userInfoRepo;
    private final UserChatInfoRepo userChatInfoRepo;
    private final GroupInfoRepo groupInfoRepo;
    private final IdTableRepo idTableRepo;
    private final MyWebSocketHandler myWebSocketHandler;
    public GroupCreateService(IdTableRepo idTableRepo,UserInfoRepo userInfoRepo,UserChatInfoRepo userChatInfoRepo,GroupInfoRepo groupInfoRepo,MyWebSocketHandler myWebSocketHandler){
        this.userInfoRepo=userInfoRepo;
        this.userChatInfoRepo=userChatInfoRepo;
        this.groupInfoRepo=groupInfoRepo;
        this.idTableRepo=idTableRepo;
        this.myWebSocketHandler=myWebSocketHandler;
    }

    public Mono<String> creategroup(String UserId,String GroupName,String motto){
        System.out.println("grouping");
        return userInfoRepo.findByUserId(UserId)
        .flatMap(user->generateChatId()
        .flatMap(chatid->{
            LocalDateTime now = LocalDateTime.now();
            String defaultName = (user.getPrivateName() != null && !user.getPrivateName().trim().isEmpty())
                    ? user.getPrivateName().trim()
                    : user.getPublicName();
            final String initialChatName = defaultName;
            return userChatInfoRepo.insert(chatid, user.getUserId(), GroupName, initialChatName, now, now,"group","admin",null,user.getUserProfilePath())
            .flatMap(chat->{ return generateGroupId()
            .doOnNext(groupid->System.out.println("inside"+chat.getChatId()))
            .flatMap(groupid->{
                String cleanMotto = (motto != null && !motto.trim().isEmpty()) ? motto.trim() : null;
                Mono<?> insertMono = (cleanMotto != null)
                    ? groupInfoRepo.insertWithMotto(groupid, GroupName, user.getUserId(), chat.getId(), cleanMotto)
                    : groupInfoRepo.insert(groupid, GroupName, user.getUserId(), chat.getId());

                return insertMono
                .then(myWebSocketHandler.announce(chatid,"created",GroupName,initialChatName,UserId))
                .thenReturn(chatid+","+groupid);
            });})
            .doOnNext(chat->System.out.println("outside"+chat));
        }))
        .switchIfEmpty(Mono.just("failed"));
    }

    public Mono<String> creategroup(String UserId,String GroupName){
        return creategroup(UserId, GroupName, null);
    }

    public Mono<String> updategroup(String GroupId, ChatBoxReturnDTO info) {
        return updategroup(GroupId, info, null, null);
    }

    public Mono<String> updategroup(String GroupId, ChatBoxReturnDTO info, String adminName, String adminId) {
        return groupInfoRepo.findByGroupId(GroupId)
            .flatMap(oldGroup -> {
                return userChatInfoRepo.findByChatId(oldGroup.getChatId())
                    .flatMap(chat -> {
                        String chatId = chat.getChatId();

                        String effectiveAdminId = (adminId != null && !adminId.trim().isEmpty())
                            ? adminId.trim()
                            : (oldGroup.getCreatedBy() != null ? oldGroup.getCreatedBy() : "admin");

                        Mono<String> resolveAdminName = (adminName != null && !adminName.trim().isEmpty())
                            ? Mono.just(adminName.trim())
                            : (effectiveAdminId != null
                                ? userChatInfoRepo.getUsername(chatId, effectiveAdminId)
                                    .switchIfEmpty(userInfoRepo.findByUserId(effectiveAdminId).map(u -> u.getPublicName()))
                                    .defaultIfEmpty("Admin")
                                : Mono.just("Admin"));

                        String newName = (info.getChatName() != null && !info.getChatName().trim().isEmpty()) ? info.getChatName().trim() : null;
                        String newProfile = (info.getProfile() != null && !info.getProfile().trim().isEmpty()) ? info.getProfile().trim() : null;
                        String newMotto = (info.getMotto() != null && !info.getMotto().trim().isEmpty()) ? info.getMotto().trim() : null;

                        boolean nameChanged = newName != null && !newName.equals(oldGroup.getGroupName());
                        boolean profileChanged = newProfile != null && !newProfile.equals(oldGroup.getGroupProfilePath());
                        boolean mottoChanged = newMotto != null && !newMotto.equals(oldGroup.getMotto());

                        return groupInfoRepo.updateWithMotto(newName, newProfile, newMotto, GroupId)
                            .flatMap(updatedGroup -> resolveAdminName.flatMap(effectiveAdminName -> {
                                Mono<Void> announceChain = Mono.empty();

                                if (nameChanged) {
                                    announceChain = announceChain.then(
                                        myWebSocketHandler.announce(chatId, "group_name", newName, effectiveAdminName, effectiveAdminId)
                                    );
                                }
                                if (profileChanged) {
                                    announceChain = announceChain.then(
                                        myWebSocketHandler.announce(chatId, "group_profile", "", effectiveAdminName, effectiveAdminId)
                                    );
                                }
                                if (mottoChanged) {
                                    announceChain = announceChain.then(
                                        myWebSocketHandler.announce(chatId, "group_description", "", effectiveAdminName, effectiveAdminId)
                                    );
                                }

                                return announceChain.thenReturn("updated");
                            }));
                    });
            })
            .switchIfEmpty(Mono.just("error"));
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
        .flatMap(userChat -> {
            UserSearchResult user = new UserSearchResult();
            user.setUserId(userChat.getUserId());
            user.setUserName(userChat.getUserName());
            user.setRole(userChat.getRole());
            user.setProfile(userChat.getUserProfile());

            return userInfoRepo.findByUserId(userChat.getUserId())
                .map(u -> {
                    user.setUsername(u.getPublicName());
                    user.setDisplayName(u.getPrivateName());
                    user.setLastDisplayNameChangeAt(u.getLastDisplayNameChangeAt());
                    return user;
                })
                .defaultIfEmpty(user);
        });
    }

    
    

}
