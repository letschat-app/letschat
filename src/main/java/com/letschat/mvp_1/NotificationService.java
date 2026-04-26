package com.letschat.mvp_1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.letschat.mvp_1.Repositories.EventInfoRepo;
import com.letschat.mvp_1.Repositories.FCMRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

@Component
public class NotificationService {

    private static final int MAX_MESSAGES = 5;

    // ================= CACHE =================
    private final Map<String, ChatState> store = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> tokenCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> eventDedup = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FCMRepo fcmRepo;
    private final EventInfoRepo eventInfoRepo;
    private final UserChatInfoRepo userChatInfoRepo;

    public NotificationService(FCMRepo fcmRepo,EventInfoRepo eventInfoRepo,UserChatInfoRepo userChatInfoRepo) {
        this.fcmRepo = fcmRepo;
        this.eventInfoRepo=eventInfoRepo;
        this.userChatInfoRepo=userChatInfoRepo;
    }

    // ================= CHAT STATE =================
    public static class ChatState {
        public int unread;
        public long lastUpdated = System.currentTimeMillis();
        public List<String> messages = new ArrayList<>();
    }

    private String chatKey(String userId, String chatId) {
        return userId + ":" + chatId;
    }

    private String eventKey(String userId, Long eventId, String type) {
        return userId + ":" + eventId + ":" + type;
    }

    // ================= TOKEN CACHE =================
    public void putToken(String userId, String token) {
        tokenCache
                .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(token);
    }

    public void removeToken(String userId, String token) {
        Set<String> set = tokenCache.get(userId);
        if (set != null) {
            set.remove(token);
            if (set.isEmpty()) tokenCache.remove(userId);
        }
    }

    public Flux<String> getTokens(String userId) {
        Set<String> cached = tokenCache.get(userId);
        if (cached != null && !cached.isEmpty()) {
            return Flux.fromIterable(cached);
        }

        return fcmRepo.getTokens(userId)
                .doOnNext(t ->
                        tokenCache
                                .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                                .add(t)
                );
    }

    // ================= CHAT LOGIC =================
    public ChatState addMessage(String userId, String chatId, String message) {
        String k = chatKey(userId, chatId);

        store.putIfAbsent(k, new ChatState());
        ChatState state = store.get(k);

        synchronized (state) {
            state.unread++;
            state.lastUpdated = System.currentTimeMillis();
            state.messages.add(message);

            if (state.messages.size() > MAX_MESSAGES) {
                state.messages.remove(0);
            }
            return state;
        }
    }

    public void clear(String userId, String chatId) {
        String k = chatKey(userId, chatId);
        store.remove(k);

        ScheduledFuture<?> f = timers.remove(k);
        if (f != null) f.cancel(false);
    }

    // ================= CHAT NOTIFY =================
    // ✅ fully reactive — no collectList, no doOnNext for logic, no nested subscribe
    public Mono<Void> notifyUser(String userId, String chatId,
                                 String title, String msg, String type) {

        return getTokens(userId)
                .flatMap(token ->
                        Mono.fromRunnable(() ->
                                processChat(userId, chatId, title, msg, token)
                        ).subscribeOn(Schedulers.boundedElastic())
                )
                .then();
    }

    // ================= DEBOUNCE =================
    // ✅ sendChat() called directly inside Mono.fromRunnable — no sendAsync, no nested subscribe
    private void processChat(String userId, String chatId,
                              String title, String msg, String token) {

        String k = chatKey(userId, chatId);
        ChatState state = addMessage(userId, chatId, msg);

        ScheduledFuture<?> existing = timers.get(k);
        if (existing != null) existing.cancel(false);

        ScheduledFuture<?> future = scheduler.schedule(() -> {

            sendChat(token, chatId, title, state.unread, state.messages, userId);

            timers.remove(k);

        }, 5, TimeUnit.SECONDS);

        timers.put(k, future);
    }

    // ================= DEDUP HELPER =================
    // ✅ TTL-based — checks timestamp not just presence
    private boolean dedupCheck(String key, long ttlMs) {
        long now = System.currentTimeMillis();
        Long last = eventDedup.get(key);

        if (last != null && (now - last) < ttlMs) return false;

        eventDedup.put(key, now);
        return true;
    }

    // ================= EVENT REMINDER =================
    // ✅ no collectList — streaming per event
    // ✅ subscribe with full error handling
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void checkUpcomingEvents() {

        Instant now = Instant.now();
        Instant next = now.plus(1, ChronoUnit.HOURS);

        eventInfoRepo.findEventsBetween(now, next)
                .flatMap(event -> {

                    String k = eventKey(event.getUserId(), event.getEventId(), "1H");

                    if (!dedupCheck(k, 60 * 60 * 1000L)) return Mono.empty();

                    return getTokens(event.getUserId())
                            .flatMap(token ->
                                    Mono.fromRunnable(() ->
                                            sendEvent(
                                                    token,
                                                    String.valueOf(event.getEventId()),
                                                    event.getEventTitle(),
                                                    event.getChatId(),
                                                    event.getUserId()
                                            )
                                    ).subscribeOn(Schedulers.boundedElastic())
                            );
                })
                .doOnError(e -> System.out.println("[EVENT REMINDER] error: " + e))
                .onErrorResume(e -> Flux.empty())
                .subscribe(
                        null,
                        e -> System.out.println("[EVENT REMINDER] fatal: " + e),
                        () -> System.out.println("[EVENT REMINDER] done")
                );
    }

    // ================= DAILY 6AM =================
    // ✅ groupBy instead of collectMultimap — no full table in memory
    // ✅ subscribe with full error handling
    @Scheduled(cron = "0 15 8 * * *")
    public void dailySummary() {

        Instant start = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant end = start.plus(1, ChronoUnit.DAYS);

        eventInfoRepo.findTodayEvents(start, end)
                .groupBy(e -> e.getUserId())
                .flatMap(group -> group
                        .map(e -> e.getEventId())
                        .collectList()
                        .flatMap(titles -> {
                            String userId = group.key();
                            String k = userId + ":daily:" + LocalDate.now();

                            if (!dedupCheck(k, 24 * 60 * 60 * 1000L))
                                return Mono.empty();

                            return getTokens(userId)
                                    .flatMap(token ->
                                            Mono.fromRunnable(() ->
                                                    sendDaily(token, titles, userId)
                                            ).subscribeOn(Schedulers.boundedElastic())
                                    )
                                    .then();
                        })
                )
                .doOnError(e -> System.out.println("[DAILY SUMMARY] error: " + e))
                .onErrorResume(e -> Flux.empty())
                .subscribe(
                        null,
                        e -> System.out.println("[DAILY SUMMARY] fatal: " + e),
                        () -> System.out.println("[DAILY SUMMARY] done")
                );
    }

    // ================= TOKEN SYNC (every 2 days) =================
    @Scheduled(cron = "0 0 0 */2 * *")
    public void syncTokens() {

        fcmRepo.getAllUsersWithTokens()
                .collectList()
                .doOnNext(list -> {

                    Map<String, Set<String>> newCache = new ConcurrentHashMap<>();

                    for (var row : list) {
                        newCache
                                .computeIfAbsent(row.getUserId(), k -> ConcurrentHashMap.newKeySet())
                                .add(row.getFcmToken());
                    }

                    tokenCache.clear();
                    tokenCache.putAll(newCache);

                    System.out.println("[TOKEN SYNC] users=" + newCache.size());
                })
                .doOnError(e -> System.out.println("[TOKEN SYNC] error: " + e))
                .onErrorResume(e -> Mono.empty())
                .subscribe(
                        null,
                        e -> System.out.println("[TOKEN SYNC] fatal: " + e),
                        () -> System.out.println("[TOKEN SYNC] done")
                );
    }

    // ================= DEDUP CLEANUP (every 2 hours) =================
    // ✅ TTL eviction — removes only expired keys, not blanket clear
    @Scheduled(fixedRate = 2 * 60 * 60 * 1000)
    public void cleanEventDedup() {
        long now = System.currentTimeMillis();
        long ttl = 25 * 60 * 60 * 1000L;

        int before = eventDedup.size();
        eventDedup.entrySet().removeIf(entry -> (now - entry.getValue()) > ttl);

        int removed = before - eventDedup.size();
        if (removed > 0) {
            System.out.println("[DEDUP CLEANUP] removed=" + removed
                    + " remaining=" + eventDedup.size());
        }
    }

    // ================= STALE CHAT CLEANUP (every 30 mins) =================
    // ✅ evicts store entries idle for over 1 hour — prevents memory leak
    @Scheduled(fixedRate = 30 * 60 * 1000)
    public void cleanStaleChats() {
        long now = System.currentTimeMillis();
        long ttl = 60 * 60 * 1000L;

        int before = store.size();

        store.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                return (now - entry.getValue().lastUpdated) > ttl;
            }
        });

        int removed = before - store.size();
        if (removed > 0) {
            System.out.println("[CHAT CLEANUP] removed=" + removed + " stale entries");
        }
    }

    // ================= FIREBASE SENDS =================
    private void sendChat(String token, String chatId, String title,
                          int unread, List<String> msgs, String userId) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", "CHAT");
            data.put("chatId", chatId);
            data.put("title", title);
            data.put("unread", String.valueOf(unread));
            data.put("msgs", objectMapper.writeValueAsString(msgs));

            FirebaseMessaging.getInstance()
                    .send(Message.builder().putAllData(data).setToken(token).build());

            System.out.println("[CHAT] sent to " + userId);

        } catch (Exception e) {
            System.out.println("[CHAT] error: " + e);
        }
    }

    private void sendEvent(String token, String eventId, String title,
                           String chatId, String userId) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", "EVENT");
            data.put("eventId", eventId);
            data.put("title", title);
            data.put("chatId", chatId);

            FirebaseMessaging.getInstance()
                    .send(Message.builder().putAllData(data).setToken(token).build());

            System.out.println("[EVENT] sent to " + userId);

        } catch (Exception e) {
            System.out.println("[EVENT] error: " + e);
        }
    }

    private void sendDaily(String token, List<Long> events, String userId) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", "DAILY");
            data.put("events", objectMapper.writeValueAsString(events));
            data.put("date", LocalDate.now().toString());

            FirebaseMessaging.getInstance()
                    .send(Message.builder().putAllData(data).setToken(token).build());

            System.out.println("[DAILY] sent to " + userId);

        } catch (Exception e) {
            System.out.println("[DAILY] error: " + e);
        }
    }
}