package com.letschat.mvp_1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.letschat.mvp_1.Repositories.AssignmentRepo;
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
import java.time.ZoneId;
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
    private final ConcurrentHashMap<String, Boolean> muteCache = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FCMRepo fcmRepo;
    private final EventInfoRepo eventInfoRepo;
    private final UserChatInfoRepo userChatInfoRepo;
    private final AssignmentRepo assignmentRepo;

    public NotificationService(FCMRepo fcmRepo,EventInfoRepo eventInfoRepo,UserChatInfoRepo userChatInfoRepo,AssignmentRepo assignmentRepo) {
        this.fcmRepo = fcmRepo;
        this.eventInfoRepo=eventInfoRepo;
        this.userChatInfoRepo=userChatInfoRepo;
        this.assignmentRepo=assignmentRepo;
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

    // ================= MUTE HELPER =================
    public void updateMuteCache(String userId, String chatId, boolean isMuted) {
        muteCache.put(userId + ":" + chatId, isMuted);
    }

    public Mono<Boolean> isChatMuted(String userId, String chatId) {
        String k = userId + ":" + chatId;
        Boolean cached = muteCache.get(k);
        if (cached != null) {
            return Mono.just(cached);
        }
        return userChatInfoRepo.isChatMuted(chatId, userId)
                .defaultIfEmpty(false)
                .doOnNext(val -> muteCache.put(k, val));
    }

    // ================= CHAT NOTIFY =================
    public Mono<Void> notifyUser(String userId, String chatId,
                                 String title, String msg, String type) {
        return notifyUser(userId, chatId, title, msg, type, false, null);
    }

    public Mono<Void> notifyUser(String userId, String chatId,
                                 String title, String msg, String type,
                                 boolean isReplyToUser, String repliedTo) {

        return isChatMuted(userId, chatId)
                .flatMap(isMuted -> {
                    if (Boolean.TRUE.equals(isMuted)) {
                        if (!isReplyToUser) {
                            // 🛑 STOPPED AT SERVER: Chat is globally muted for this user!
                            return Mono.empty();
                        }
                        // ⚡ REPLY OVERRIDE: Muted, but direct reply to user!
                        System.out.println("[CHAT NOTIFY] Reply override triggered for muted user " + userId + " in chat " + chatId);
                    }

                    return Mono.fromRunnable(() ->
                            processChat(userId, chatId, title, msg, type, isReplyToUser, repliedTo)
                    ).subscribeOn(Schedulers.boundedElastic()).then();
                });
    }

    // ================= DEBOUNCE =================
    private void processChat(String userId, String chatId,
                             String title, String msg, String type,
                             boolean isReplyToUser, String repliedTo) {

        String k = chatKey(userId, chatId);
        ChatState state = addMessage(userId, chatId, msg);

        ScheduledFuture<?> existing = timers.get(k);
        if (existing != null) existing.cancel(false);

        // 30 seconds for room, 5 seconds for normal chats
        long debounceSeconds = "room".equalsIgnoreCase(type) ? 30L : 5L;

        ScheduledFuture<?> future = scheduler.schedule(() -> {

            int unreadToSend;
            List<String> msgsToSend;
            synchronized (state) {
                unreadToSend = state.unread;
                msgsToSend = new ArrayList<>(state.messages);
            }

            getTokens(userId)
                    .collectList()
                    .subscribe(tokens -> {
                        if (tokens != null && !tokens.isEmpty()) {
                            for (String token : tokens) {
                                sendChat(token, chatId, title, unreadToSend, msgsToSend, userId, type, repliedTo);
                            }
                        }
                    });

            timers.remove(k);

        }, debounceSeconds, TimeUnit.SECONDS);

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
    // ✅ runs every 10 minutes
    // ✅ evaluates dynamic reminder_offset (60 min default, 120 min, 1 day, custom 15 min)
    // ✅ filters out cancelled events and disabled reminders
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void checkUpcomingEvents() {

        Instant now = Instant.now();
        Instant maxFuture = now.plus(25, ChronoUnit.HOURS); // covers up to 1-day reminders (24h)

        eventInfoRepo.findEventsForReminderEvaluation(now, maxFuture)
                .filter(event -> {
                    if (event.getStartTime() == null) return false;
                    if ("cancelled".equalsIgnoreCase(event.getAction())) return false;
                    if (Boolean.FALSE.equals(event.getReminderEnabled())) return false;

                    int offsetMins = event.getReminderOffset() != null ? event.getReminderOffset() : 60;
                    Instant reminderTargetTime = event.getStartTime().minus(offsetMins, ChronoUnit.MINUTES);

                    // Check if current scheduler run is within 10 minutes of the target reminder time
                    long diffSeconds = ChronoUnit.SECONDS.between(now, reminderTargetTime);
                    return diffSeconds >= -600 && diffSeconds <= 600;
                })
                .flatMap(event -> {
                    int offset = event.getReminderOffset() != null ? event.getReminderOffset() : 60;
                    String k = eventKey(event.getUserId(), event.getEventId(), offset + "M");

                    if (!dedupCheck(k, 15 * 60 * 1000L)) return Mono.empty();

                    String startTimeStr = event.getStartTime() != null ? event.getStartTime().toString() : "";
                    String eventIdStr = String.valueOf(event.getEventId());
                    String eventTitle = event.getEventTitle() != null ? event.getEventTitle() : "Upcoming Event";
                    String eventChatId = event.getChatId() != null ? event.getChatId() : "";

                    Mono<String> chatNameMono = (eventChatId != null && !eventChatId.isEmpty() && !"0".equals(eventChatId))
                            ? userChatInfoRepo.findChatNameByChatId(eventChatId).defaultIfEmpty("LetsChat Event")
                            : Mono.just("LetsChat Event");

                    return chatNameMono.flatMap(chatName ->
                            getTokens(event.getUserId())
                                    .flatMap(token ->
                                            Mono.fromRunnable(() ->
                                                    sendEvent(
                                                            token,
                                                            eventIdStr,
                                                            eventTitle,
                                                            eventChatId,
                                                            event.getUserId(),
                                                            startTimeStr,
                                                            chatName,
                                                            event.getMeetingType(),
                                                            event.getMeetingLink(),
                                                            event.getLocation()
                                                    )
                                            ).subscribeOn(Schedulers.boundedElastic())
                                    )
                                    .then()
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

    // ================= DAILY 7AM IST =================
    // ✅ groupBy instead of collectMultimap — no full table in memory
    // ✅ runs at 7:00 AM Indian Standard Time (Asia/Kolkata)
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Kolkata")
    public void dailySummary() {

        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(istZone);
        Instant start = today.atStartOfDay(istZone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(istZone).toInstant();

        eventInfoRepo.findTodayEvents(start, end)
                .groupBy(e -> e.getUserId())
                .flatMap(group -> group
                        .map(e -> e.getEventId())
                        .collectList()
                        .flatMap(titles -> {
                            String userId = group.key();
                            String k = userId + ":daily:" + today;

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

    // ================= ASSIGNMENT MISSING 7AM IST =================
    // ✅ runs at 7:00 AM Indian Standard Time (Asia/Kolkata)
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Kolkata")
    public void assignmentMissingSummary() {

        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(istZone);

        assignmentRepo.findMissingAssignments()
                .flatMap(dto -> {

                    if (dto.getAssignmentIds() == null || dto.getAssignmentIds().isEmpty()) {
                        return Mono.empty();
                    }

                    String k = dto.getUserId() + ":assignment:" + today;

                    if (!dedupCheck(k, 24 * 60 * 60 * 1000L)) {
                        return Mono.empty();
                    }

                    return getTokens(dto.getUserId())
                            .flatMap(token ->
                                    Mono.fromRunnable(() ->
                                            sendAssignmentNotification(
                                                    token,
                                                    dto.getUserId(),
                                                    dto.getAssignmentIds()
                                            )
                                    ).subscribeOn(Schedulers.boundedElastic())
                            )
                            .then();
                })
                .doOnError(e -> System.out.println("[ASSIGNMENT SUMMARY] error: " + e))
                .onErrorResume(e -> Flux.empty())
                .subscribe();
    }

    // ================= TOKEN SYNC (every 7 days at midnight) =================
    @Scheduled(cron = "0 0 0 */7 * *", zone = "Asia/Kolkata")
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
                          int unread, List<String> msgs, String userId,
                          String chatType, String repliedTo) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", "CHAT");
            data.put("chatId", chatId);
            data.put("title", title);
            data.put("unread", "room".equalsIgnoreCase(chatType) ? "1" : String.valueOf(unread));
            data.put("msgs", objectMapper.writeValueAsString(msgs));
            if (chatType != null) data.put("chatType", chatType);
            if (repliedTo != null && !repliedTo.isEmpty()) data.put("repliedTo", repliedTo);

            FirebaseMessaging.getInstance()
                    .send(Message.builder().putAllData(data).setToken(token).build());

            System.out.println("[CHAT] sent to " + userId + " (type=" + chatType + ")");

        } catch (Exception e) {
            System.out.println("[CHAT] error: " + e);
        }
    }

    private void sendEvent(String token, String eventId, String title,
                           String chatId, String userId, String startTime, String chatName) {
        sendEvent(token, eventId, title, chatId, userId, startTime, chatName, null, null, null);
    }

    private void sendEvent(String token, String eventId, String title,
                           String chatId, String userId, String startTime, String chatName,
                           String meetingType, String meetingLink, String location) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", "EVENT");
            data.put("eventId", eventId != null ? eventId : "");
            data.put("title", title != null ? title : "Upcoming Event");
            data.put("chatId", chatId != null ? chatId : "");
            data.put("chatName", chatName != null ? chatName : "LetsChat Event");
            data.put("startTime", startTime != null ? startTime : "");
            data.put("meetingType", meetingType != null ? meetingType : "");
            data.put("meetingLink", meetingLink != null ? meetingLink : "");
            data.put("location", location != null ? location : "");

            FirebaseMessaging.getInstance()
                    .send(Message.builder().putAllData(data).setToken(token).build());

            System.out.println("[EVENT] sent to " + userId + " for " + chatName);

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

    private void sendAssignmentNotification(String token,
                                        String userId,
                                        List<Long> assignmentIds) {
        try {
            Map<String, String> data = new HashMap<>();
            data.put("type", "ASSIGNMENT_MISSING");
            data.put("userId", userId);
            data.put("assignments", objectMapper.writeValueAsString(assignmentIds));

            FirebaseMessaging.getInstance()
                .send(Message.builder()
                        .putAllData(data)
                        .setToken(token)
                        .build());

            System.out.println("[ASSIGNMENT] sent to " + userId);

        } catch (Exception e) {
            System.out.println("[ASSIGNMENT] error: " + e.getMessage());
        }
    }

    public void sendEventChange(String userId, String eventId, String title, String changeSummary, String startTime) {
        if (userId == null || userId.isEmpty()) return;
        getTokens(userId)
                .flatMap(token ->
                        Mono.fromRunnable(() -> {
                            try {
                                Map<String, String> data = new HashMap<>();
                                data.put("type", "EVENT_CHANGED");
                                data.put("eventId", eventId != null ? eventId : "");
                                data.put("title", title != null ? ("Event Updated: " + title) : "Event Details Changed");
                                data.put("changeSummary", changeSummary != null ? changeSummary : "Event details have been updated.");
                                data.put("startTime", startTime != null ? startTime : "");

                                FirebaseMessaging.getInstance()
                                        .send(Message.builder().putAllData(data).setToken(token).build());

                                System.out.println("[EVENT_CHANGED] sent to " + userId + " for event " + eventId);
                            } catch (Exception e) {
                                System.out.println("[EVENT_CHANGED] error: " + e);
                            }
                        }).subscribeOn(Schedulers.boundedElastic())
                )
                .subscribe(null, e -> System.out.println("[EVENT_CHANGED] subscription error: " + e));
    }
}