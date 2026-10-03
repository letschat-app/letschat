package com.letschat.mvp_1.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.letschat.mvp_1.Repositories.MessageInfoRepo;

import reactor.core.publisher.Mono;

@Component
public class AdminService {
    private final MessageInfoRepo messageInfoRepo;
    public AdminService(MessageInfoRepo messageInfoRepo){
        this.messageInfoRepo=messageInfoRepo;
    }

    // ===== USER SIGNALS =====
    private final AtomicInteger totalUsers = new AtomicInteger(0);
    private final AtomicInteger newUsersToday = new AtomicInteger(0);

    private final Set<String> activeUsersNow = ConcurrentHashMap.newKeySet();
    private final Set<String> activeUsersToday = ConcurrentHashMap.newKeySet();

    // ===== MESSAGE SIGNALS =====
    private final AtomicInteger messagesLastMinute = new AtomicInteger(0);
    private final AtomicInteger messagesLastHour = new AtomicInteger(0);
    private final AtomicInteger messagesToday = new AtomicInteger(0);
    private final AtomicInteger peakMessagesPerMinute = new AtomicInteger(0);

    private final Map<String, Set<String>> dailyActiveUsers = new ConcurrentHashMap<>();
    // ===== USER EVENTS =====

    public void onUserRegistered(String userId) {
        totalUsers.incrementAndGet();
        newUsersToday.incrementAndGet();
    }

    public void onUserConnected(String userId) {
        activeUsersNow.add(userId);
        activeUsersToday.add(userId);
        dailyActiveUsers.computeIfAbsent(java.time.LocalDate.now().toString(), k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    public void onUserDisconnected(String userId) {
        activeUsersNow.remove(userId);
    }

    public void onUserActivity(String userId) {
        activeUsersToday.add(userId);
        dailyActiveUsers.computeIfAbsent(java.time.LocalDate.now().toString(), k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    // ===== MESSAGE EVENTS =====

    public void onMessageSent() {
        int currentMinute = messagesLastMinute.incrementAndGet();
        messagesLastHour.incrementAndGet();
        messagesToday.incrementAndGet();

        peakMessagesPerMinute.updateAndGet(prev -> Math.max(prev, currentMinute));
    }

    //Retension
    public double getRetention(String fromDate, String toDate) {

    Set<String> fromUsers = dailyActiveUsers.getOrDefault(fromDate, Set.of());
    Set<String> toUsers = dailyActiveUsers.getOrDefault(toDate, Set.of());

    if (fromUsers.isEmpty()) return 0.0;

    long retained = fromUsers.stream()
            .filter(toUsers::contains)
            .count();

    return (retained * 100.0) / fromUsers.size();
    }
    // ===== GETTERS =====

    public Map<String, Object> getAllSignals() {
        return Map.of(
            // user signals
            "totalUsers", totalUsers.get(),
            "newUsersToday", newUsersToday.get(),
            "activeUsersNow", activeUsersNow.size(),
            "activeUsersToday", activeUsersToday.size(),

            // message signals
            "messagesLastMinute", messagesLastMinute.get(),
            "messagesLastHour", messagesLastHour.get(),
            "messagesToday", messagesToday.get(),
            "peakMessagesPerMinute", peakMessagesPerMinute.get()
        );
    }

    // ===== RESET LOGIC =====

    // every minute (exact)
    @Scheduled(cron = "0 * * * * ?")
    public void resetMinute() {
        messagesLastMinute.set(0);
    }

    // every hour (exact)
    @Scheduled(cron = "0 0 * * * ?")
    public void resetHour() {
        messagesLastHour.set(0);
    }

    // daily reset
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDaily() {
        // user
        newUsersToday.set(0);
        activeUsersToday.clear();

        // message
        messagesToday.set(0);
        peakMessagesPerMinute.set(0);
    }

    public Mono<Map<String, Object>> getSpaceAdoptionStats() {

        return Mono.zip(
            messageInfoRepo.getTotalUsers().defaultIfEmpty(0L),
            messageInfoRepo.getDefaultOnlyUsers().defaultIfEmpty(0L),
            messageInfoRepo.getMultiSpaceUsers().defaultIfEmpty(0L),
            messageInfoRepo.getCustomSpaceUsers().defaultIfEmpty(0L)
        ).map(t -> {

            long total = t.getT1() != null ? t.getT1() : 0L;
            long defaultOnly = t.getT2() != null ? t.getT2() : 0L;
            long multi = t.getT3() != null ? t.getT3() : 0L;
            long custom = t.getT4() != null ? t.getT4() : 0L;

            Map<String, Object> res = new ConcurrentHashMap<>();
            res.put("totalUsers", total);
            res.put("defaultOnlyUsers", defaultOnly);
            res.put("multiSpaceUsers", multi);
            res.put("customSpaceUsers", custom);

            res.put("defaultOnlyPercent", total == 0 ? 0.0 : (defaultOnly * 100.0 / total));
            res.put("multiSpacePercent", total == 0 ? 0.0 : (multi * 100.0 / total));
            res.put("customSpacePercent", total == 0 ? 0.0 : (custom * 100.0 / total));

            return res;
        }).defaultIfEmpty(Map.of(
            "totalUsers", 0L,
            "defaultOnlyUsers", 0L,
            "multiSpaceUsers", 0L,
            "customSpaceUsers", 0L,
            "defaultOnlyPercent", 0.0,
            "multiSpacePercent", 0.0,
            "customSpacePercent", 0.0
        ));
    }
}