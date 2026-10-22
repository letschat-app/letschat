package com.letschat.mvp_1.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.UserSearchResult;
import com.letschat.mvp_1.Models.PulseInfo;
import com.letschat.mvp_1.Models.UserInfo;
import com.letschat.mvp_1.Repositories.PulseInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;
import com.letschat.mvp_1.Repositories.UserPulseStreakRepo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class UserSearchService {

    private final UserInfoRepo userInfoRepo;
    private final UserPulseStreakRepo userPulseStreakRepo;
    private final PulseInfoRepo pulseInfoRepo;

    public UserSearchService(UserInfoRepo userInfoRepo, UserPulseStreakRepo userPulseStreakRepo, PulseInfoRepo pulseInfoRepo){
        this.userInfoRepo = userInfoRepo;
        this.userPulseStreakRepo = userPulseStreakRepo;
        this.pulseInfoRepo = pulseInfoRepo;
    }

    private Mono<UserSearchResult> buildSearchResult(UserInfo user) {
        UserSearchResult dto = new UserSearchResult();
        dto.setUserId(user.getUserId());
        dto.setUserName(user.getPrivateName() != null && !user.getPrivateName().trim().isEmpty() ? user.getPrivateName() : user.getPublicName());
        dto.setUsername(user.getPublicName());
        dto.setDisplayName(user.getPrivateName());
        dto.setLastDisplayNameChangeAt(user.getLastDisplayNameChangeAt());
        dto.setProfile(user.getUserProfilePath());

        Mono<Integer> streakMono = userPulseStreakRepo.findById(user.getUserId())
                .map(s -> s.getEffectiveStreak(LocalDate.now()))
                .onErrorReturn(0)
                .defaultIfEmpty(0);

        Mono<List<PulseInfo>> activePulsesMono = pulseInfoRepo.findActiveProfilePulses(user.getUserId(), true, LocalDateTime.now())
                .collectList();

        return Mono.zip(streakMono, activePulsesMono)
                .map(tuple -> {
                    int streak = tuple.getT1();
                    List<PulseInfo> activePulses = tuple.getT2();
                    user.setPulseStreak(streak);
                    dto.setPulseStreak(streak);
                    dto.setHasActivePulse(!activePulses.isEmpty());
                    dto.setAllSeen(false);
                    return dto;
                });
    }

    public Mono<UserSearchResult> search(String UserId){
        System.out.println("searching:" + UserId);
        return userInfoRepo.findByUserId(UserId)
                .flatMap(this::buildSearchResult);
    }

    public Flux<UserSearchResult> explore(){
        System.out.println("explore");
        return userInfoRepo.findAll()
                .flatMap(this::buildSearchResult);
    }

    public Mono<UserSearchResult> getme(String userid){
        return userInfoRepo.getme(userid)
                .flatMap(this::buildSearchResult);
    }
}
