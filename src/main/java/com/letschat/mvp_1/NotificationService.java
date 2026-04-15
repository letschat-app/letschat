package com.letschat.mvp_1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;

import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.letschat.mvp_1.Repositories.FCMRepo;
import java.util.*;
import java.util.concurrent.*;


@Component
public class NotificationService {

    private static final int MAX_MESSAGES = 5;

    // 🔹 In-memory message aggregation
    private final Map<String, ChatState> store = new ConcurrentHashMap<>();

    // 🔹 Debounce timers
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2);

    // 🔹 Token cache
    private final Map<String, Set<String>> tokenCache = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final FCMRepo fcmRepo;

    public NotificationService(FCMRepo fcmRepo) {
        this.fcmRepo = fcmRepo;
    }

    // 🔹 Chat state
    public static class ChatState {
        public int unread;
        public List<String> messages = new ArrayList<>();
    }

    private String key(String userId, String chatId) {
        return userId + ":" + chatId;
    }

    // =========================
    // 🔥 TOKEN CACHE (REACTIVE)
    // =========================

    public Flux<String> getTokens(String userId) {

        Set<String> cached = tokenCache.get(userId);

        // ✅ Cache hit
        if (cached != null && !cached.isEmpty()) {
            return Flux.fromIterable(cached);
        }

        // 🔥 Cache miss → DB
        return fcmRepo.getTokens(userId)
                .doOnNext(token -> {
                    tokenCache
                            .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                            .add(token);
                });
    }

    // public Mono<Void> addToken(String userId, String token) {

    //     tokenCache
    //             .computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
    //             .add(token);

    //     return fcmRepo.saveToken(userId, token).then();
    // }

    // public Mono<Void> removeToken(String userId, String token) {

    //     Set<String> tokens = tokenCache.get(userId);

    //     if (tokens != null) {
    //         tokens.remove(token);
    //     }

    //     return fcmRepo.deleteToken(userId, token).then();
    // }

    // =========================
    // 🔥 MESSAGE AGGREGATION
    // =========================

    public ChatState addMessage(String userId, String chatId, String message) {

        String k = key(userId, chatId);

        store.putIfAbsent(k, new ChatState());

        ChatState state = store.get(k);

        synchronized (state) {

            state.unread++;

            state.messages.add(message);

            if (state.messages.size() > MAX_MESSAGES) {
                state.messages.remove(0);
            }

            return state;
        }
    }

    public void clear(String userId, String chatId) {

        store.remove(key(userId, chatId));

        ScheduledFuture<?> future = timers.remove(key(userId, chatId));

        if (future != null) {
            future.cancel(false);
        }
    }

    // =========================
    // 🔥 MAIN ENTRY METHOD
    // =========================

    public Mono<Void> notifyUser(
            String userId,
            String chatId,
            String senderName,
            String messageContent,
            String type
            
    ) {

        return getTokens(userId)
                .flatMap(token -> {

                    processNotification(
                            userId,
                            chatId,
                            senderName,
                            messageContent,
                            token
                    );

                    return Mono.empty();
                })
                .then();
    }

    // =========================
    // 🔥 DEBOUNCE LOGIC
    // =========================

    public void processNotification(
            String receiverId,
            String chatId,
            String senderName,
            String messageContent,
            String deviceToken
    ) {

        String k = key(receiverId, chatId);

        // 1️⃣ aggregate
        ChatState state = addMessage(receiverId, chatId, messageContent);

        // 2️⃣ cancel old timer
        ScheduledFuture<?> existing = timers.get(k);
        if (existing != null) {
            existing.cancel(false);
        }

        // 3️⃣ schedule new (5 sec)
        ScheduledFuture<?> future = scheduler.schedule(() -> {

            sendAsync(
                    deviceToken,
                    chatId,
                    senderName,
                    state.unread,
                    state.messages,
                    receiverId
            ).subscribe();

            timers.remove(k);

        }, 5, TimeUnit.SECONDS);

        timers.put(k, future);
    }

    // =========================
    // 🔥 FIREBASE SEND
    // =========================

    private void send(
            String token,
            String chatId,
            String title,
            int unread,
            List<String> messages,
            String userId
    ) {

        try {

            Map<String, String> data = new HashMap<>();

            data.put("chatId", chatId);
            data.put("title", title);
            data.put("unread", String.valueOf(unread));
            data.put("msgs", objectMapper.writeValueAsString(messages));
            data.put("url", "/chat/" + chatId);

            Message msg = Message.builder()
                    .putAllData(data)
                    .setToken(token)
                    .build();

            FirebaseMessaging.getInstance().send(msg);
            System.out.println("notification sent");
        } catch (Exception e) {

            System.out.println("FCM error: " + e);
        }
    }

    // 🔹 Reactive wrapper
    public Mono<Void> sendAsync(
            String token,
            String chatId,
            String title,
            int unread,
            List<String> messages,
            String userId
    ) {
        return Mono.fromRunnable(() ->
                send(token, chatId, title, unread, messages, userId)
        ).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
