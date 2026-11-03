package com.letschat.mvp_1;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import java.util.stream.Collectors;
import java.time.ZonedDateTime;
import java.time.Duration;

import jakarta.annotation.PreDestroy;



import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.firebase.messaging.FirebaseMessaging;

import com.google.firebase.messaging.FirebaseMessagingException;

import com.google.firebase.messaging.Message;

import com.letschat.mvp_1.DTOs.MissingAssignmentItemDTO;

import com.letschat.mvp_1.Models.EventInfo;

import com.letschat.mvp_1.Repositories.AssignmentRepo;

import com.letschat.mvp_1.Repositories.EventInfoRepo;

import com.letschat.mvp_1.Repositories.FCMRepo;

import com.letschat.mvp_1.Repositories.UserChatInfoRepo;



import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

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



    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private static final int MAX_MESSAGES = 5;

    public static final long CHAT_STATE_EXPIRY_MS = TimeUnit.DAYS.toMillis(7); // 7 days (604_800_000 ms)

    public static final long CHAT_STATE_CLEANUP_INTERVAL_HOURS = 24L; // 24 hours



    // ================= CACHE =================

    private final Map<String, ChatState> store = new ConcurrentHashMap<>();

    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> tokenCache = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> eventDedup = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Boolean> muteCache = new ConcurrentHashMap<>();



    private final ScheduledExecutorService scheduler =

            Executors.newScheduledThreadPool(8);



    private final ObjectMapper objectMapper = new ObjectMapper();

    private final FCMRepo fcmRepo;

    private final EventInfoRepo eventInfoRepo;

    private final UserChatInfoRepo userChatInfoRepo;

    private final AssignmentRepo assignmentRepo;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.r2dbc.core.DatabaseClient databaseClient;



    public NotificationService(FCMRepo fcmRepo, EventInfoRepo eventInfoRepo, UserChatInfoRepo userChatInfoRepo, AssignmentRepo assignmentRepo) {

        this.fcmRepo = fcmRepo;

        this.eventInfoRepo = eventInfoRepo;

        this.userChatInfoRepo = userChatInfoRepo;

        this.assignmentRepo = assignmentRepo;
        scheduler.scheduleAtFixedRate(this::cleanupCaches, 30, 30, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(this::cleanupChatStates, CHAT_STATE_CLEANUP_INTERVAL_HOURS, CHAT_STATE_CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS);

    }



    // ================= CACHE CLEANUP =================

    public void cleanupCaches() {
        try {
            long now = System.currentTimeMillis();
            eventDedup.entrySet().removeIf(e -> now - e.getValue() > 86_400_000L);
            muteCache.clear();
            tokenCache.clear(); // Refresh multi-device token cache from DB every 30 mins
        } catch (Exception e) {
            logger.warn("[PUSH] Cache cleanup warning: {}", e.getMessage());
        }
    }

    public void cleanupChatStates() {
        try {
            long now = System.currentTimeMillis();
            int removedCount = 0;
            for (Map.Entry<String, ChatState> entry : store.entrySet()) {
                String chatKey = entry.getKey();
                ChatState state = entry.getValue();
                if (state == null) continue;

                ScheduledFuture<?> timer = timers.get(chatKey);
                if (timer != null && timer.isDone()) {
                    timers.remove(chatKey, timer);
                    timer = null;
                }
                if (timer != null) {
                    continue; // Active timer waiting in timers - do not touch
                }

                synchronized (state) {
                    if (state.batchCount > 0) {
                        continue; // Pending debounce waiting to be sent - do not touch
                    }

                    if (now - state.lastUpdated > CHAT_STATE_EXPIRY_MS) {
                        ScheduledFuture<?> activeTimer = timers.get(chatKey);
                        if (activeTimer != null && !activeTimer.isDone()) {
                            continue;
                        }
                        store.remove(chatKey, state);
                        removedCount++;
                    }
                }
            }
            logger.info("[PUSH] Chat state cleanup completed: removed {} idle chat states older than 7 days", removedCount);
        } catch (Exception e) {
            logger.warn("[PUSH] Chat state cleanup warning: {}", e.getMessage());
        }
    }

    public static class ChatState {

        public int unread;

        public long lastUpdated = System.currentTimeMillis();

        public List<String> messages = new ArrayList<>();

        public long firstPendingAt = 0;

        public String lastSenderId = "";

        public String lastSenderName = "";

        public String lastMsgType = "text";

        public String lastMsgId = "";

        public String lastSpaceId = "0";

        public String lastPreview = "";

        public long lastTs = 0;

        public int batchCount = 0;

        public List<Map<String, String>> items = new ArrayList<>();

        public Set<String> announcedMsgIds = new HashSet<>();

    }



    private String chatKey(String userId, String chatId) {

        return userId + ":" + chatId;

    }



    private String eventKey(String userId, Long eventId, String type) {

        return userId + ":" + eventId + ":" + type;

    }



    // ================= TOKEN CACHE =================
    public void putToken(String userId, String token) {
        if (userId == null || token == null) return;
        Set<String> set = tokenCache.get(userId);
        if (set != null) {
            set.add(token);
        }
        // If not in cache, do not create a 1-item partial set!
        // Leaving it uncached ensures getTokens(userId) queries fcmRepo for the complete set of devices.
    }

    public void removeToken(String userId, String token) {
        Set<String> set = tokenCache.get(userId);
        if (set != null) {
            set.remove(token);
            if (set.isEmpty()) tokenCache.remove(userId);
        }
    }

    public void invalidateTokenCache(String userId) {
        if (userId != null) {
            tokenCache.remove(userId);
        }
    }

    public Flux<String> getTokens(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return Flux.empty();
        }
        Set<String> cached = tokenCache.get(userId);
        if (cached != null) {
            return Flux.fromIterable(cached);
        }

        return fcmRepo.getTokens(userId)
                .collectList()
                .flatMapMany(tokens -> {
                    Set<String> set = ConcurrentHashMap.newKeySet();
                    set.addAll(tokens);
                    tokenCache.put(userId, set);
                    return Flux.fromIterable(set);
                });
    }

    // ================= CHAT LOGIC =================

    public ChatState addMessage(String userId, String chatId, String message) {

        return addMessage(userId, chatId, message, "", "", "text", "", "0", "", System.currentTimeMillis(), false);

    }



    public ChatState addMessage(String userId, String chatId, String message,

                                String senderId, String senderName, String msgType,

                                String msgId, String spaceId, String preview,

                                long ts, boolean alreadyAnnounced) {

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



            if (alreadyAnnounced) {

                if (msgId != null && !msgId.isEmpty()) {

                    state.announcedMsgIds.add(msgId);

                }

            } else {

                if (state.firstPendingAt <= 0) {

                    state.firstPendingAt = System.currentTimeMillis();

                }

                state.batchCount++;

                state.lastSenderId = senderId != null ? senderId : "";

                state.lastSenderName = senderName != null ? senderName : "";

                state.lastMsgType = msgType != null ? msgType : "text";

                state.lastMsgId = msgId != null ? msgId : "";

                state.lastSpaceId = spaceId != null ? spaceId : "0";

                state.lastPreview = preview != null ? preview : "";

                state.lastTs = ts > 0 ? ts : System.currentTimeMillis();



                Map<String, String> item = new LinkedHashMap<>();

                item.put("s", state.lastSenderName);

                item.put("t", state.lastMsgType);

                item.put("x", state.lastPreview);

                state.items.add(item);

                if (state.items.size() > MAX_MESSAGES) {

                    state.items.remove(0);

                }

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



    public void invalidateMuteCache(String userId, String chatId) {

        if (userId != null && chatId != null) {

            muteCache.remove(userId + ":" + chatId);

        } else if (userId != null) {

            muteCache.keySet().removeIf(k -> k.startsWith(userId + ":"));

        }

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

    public Mono<Void> notifyUser(

            String userId, String chatId, String legacyTitle, String chatName,

            String chatKind, String alertKind, String senderId, String senderName,

            String msgType, String msgId, String spaceId, String preview,

            long ts, boolean isMention, boolean isReplyToUser,

            String legacyMsg, String repliedTo

    ) {

        return isChatMuted(userId, chatId)

                .flatMap(isMuted -> {

                    if (!PushPolicy.shouldNotify(Boolean.TRUE.equals(isMuted), isReplyToUser, isMention)) {

                        return Mono.empty();

                    }



                    return Mono.fromRunnable(() -> {

                        processChatV2(

                                userId, chatId, legacyTitle, chatName, chatKind, alertKind,

                                senderId, senderName, msgType, msgId, spaceId, preview,

                                ts, isMention, isReplyToUser, legacyMsg, repliedTo

                        );

                    }).subscribeOn(Schedulers.boundedElastic()).then();

                });

    }



    public Mono<Void> notifyUser(String userId, String chatId,

                                 String title, String msg, String type) {

        return notifyUser(userId, chatId, title, msg, type, false, null);

    }



    public Mono<Void> notifyUser(String userId, String chatId,

                                 String title, String msg, String type,

                                 boolean isReplyToUser, String repliedTo) {

        return notifyUser(

                userId, chatId, title, title, "room".equalsIgnoreCase(type) ? "room" : "group",

                isReplyToUser ? "reply" : "message", "", "", "text", "", "0", "",

                System.currentTimeMillis(), false, isReplyToUser, msg, repliedTo

        );

    }



    // ================= DEBOUNCE (V2) =================

    private void processChatV2(

            String userId, String chatId, String legacyTitle, String chatName,

            String chatKind, String alertKind, String senderId, String senderName,

            String msgType, String msgId, String spaceId, String preview,

            long ts, boolean isMention, boolean isReplyToUser,

            String legacyMsg, String repliedTo

    ) {

        String k = chatKey(userId, chatId);



        // IMMEDIATE PUSH FOR MENTION OR REPLY (in group or room chats)

        if (("mention".equals(alertKind) || "reply".equals(alertKind)) && !"dm".equals(chatKind)) {

            ChatState state = addMessage(

                    userId, chatId, legacyMsg, senderId, senderName, msgType,

                    msgId, spaceId, preview, ts, true

            );



            Map<String, String> item = new LinkedHashMap<>();

            item.put("s", senderName != null ? senderName : "");

            item.put("t", msgType != null ? msgType : "text");

            item.put("x", preview != null ? preview : "");



            int currentUnread;

            synchronized (state) {

                currentUnread = state.unread;

            }



            Map<String, String> data = PushPayloadBuilder.buildChatData(

                    chatId,

                    legacyTitle,

                    chatName,

                    chatKind,

                    alertKind,

                    senderId,

                    senderName,

                    msgType,

                    msgId,

                    spaceId,

                    preview,

                    Collections.singletonList(item),

                    1,

                    currentUnread,

                    ts,

                    isMention,

                    isReplyToUser,

                    Collections.singletonList(legacyMsg),

                    chatKind,

                    repliedTo

            );



            getTokens(userId)

                    .collectList()

                    .subscribe(tokens -> {

                        if (tokens != null && !tokens.isEmpty()) {

                            for (String token : tokens) {

                                Message message = PushPayloadBuilder.buildChatMessage(data, token);

                                safeSend(userId, token, message, "CHAT_" + alertKind.toUpperCase());

                            }

                        }

                    });

            return;

        }



        // NORMAL MERGED DEBOUNCED PUSH

        ChatState state = addMessage(

                userId, chatId, legacyMsg, senderId, senderName, msgType,

                msgId, spaceId, preview, ts, false

        );



        long now = System.currentTimeMillis();

        long debounceDelayMs = "room".equalsIgnoreCase(chatKind) ? 30000L : 5000L;

        long maxWaitMs = "room".equalsIgnoreCase(chatKind) ? 60000L : 15000L;



        long delayMs;

        synchronized (state) {

            delayMs = PushPolicy.calculateDebounceDelayMs(now, state.firstPendingAt, debounceDelayMs, maxWaitMs);

        }



        timers.compute(k, (key, existing) -> {
            if (existing != null) {
                existing.cancel(false);
            }
            return scheduler.schedule(() -> {
                Mono.fromRunnable(() -> {
                    int unreadToSend;
                    int batchCountToSend;
                    List<Map<String, String>> itemsToSend;
                    List<String> legacyMsgsToSend;
                    String finalSenderId;
                    String finalSenderName;
                    String finalMsgType;
                    String finalMsgId;
                    String finalSpaceId;
                    String finalPreview;
                    long finalTs;

                    synchronized (state) {
                        if (state.batchCount == 0) {
                            state.firstPendingAt = 0;
                            timers.remove(k);
                            return;
                        }

                        unreadToSend = state.unread;
                        batchCountToSend = state.batchCount;
                        itemsToSend = new ArrayList<>(state.items);
                        legacyMsgsToSend = new ArrayList<>(state.messages);
                        finalSenderId = state.lastSenderId;
                        finalSenderName = state.lastSenderName;
                        finalMsgType = state.lastMsgType;
                        finalMsgId = state.lastMsgId;
                        finalSpaceId = state.lastSpaceId;
                        finalPreview = state.lastPreview;
                        finalTs = state.lastTs;

                        // Reset batch counters
                        state.batchCount = 0;
                        state.firstPendingAt = 0;
                    }

                    Map<String, String> data = PushPayloadBuilder.buildChatData(
                            chatId,
                            legacyTitle,
                            chatName,
                            chatKind,
                            "message",
                            finalSenderId,
                            finalSenderName,
                            finalMsgType,
                            finalMsgId,
                            finalSpaceId,
                            finalPreview,
                            itemsToSend,
                            batchCountToSend,
                            unreadToSend,
                            finalTs,
                            false,
                            false,
                            legacyMsgsToSend,
                            chatKind,
                            repliedTo
                    );

                    getTokens(userId)
                            .collectList()
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(tokens -> {
                                if (tokens != null && !tokens.isEmpty()) {
                                    for (String token : tokens) {
                                        Message message = PushPayloadBuilder.buildChatMessage(data, token);
                                        safeSend(userId, token, message, "CHAT_BATCH");
                                    }
                                }
                            }, err -> logger.error("[PUSH] Error retrieving tokens for debounced push to user {}", userId, err));
                }).subscribeOn(Schedulers.boundedElastic()).subscribe();
            }, delayMs, TimeUnit.MILLISECONDS);
        });

    }



    // ================= PERSISTENT MARKERS (Item R8) =================
    private Mono<Boolean> checkIfDailySummarySentToday(LocalDate date) {
        if (databaseClient == null) return Mono.just(false);
        return databaseClient.sql("SELECT COUNT(*) AS cnt FROM \"DailySummaryLog\" WHERE summary_type = 'daily' AND summary_date = :date")
                .bind("date", date)
                .map((row, metadata) -> {
                    Number count = (Number) row.get("cnt");
                    return count != null && count.longValue() > 0;
                })
                .first()
                .onErrorResume(e -> Mono.just(false));
    }

    private Mono<Boolean> checkIfAssignmentSummarySentToday(LocalDate date) {
        if (databaseClient == null) return Mono.just(false);
        return databaseClient.sql("SELECT COUNT(*) AS cnt FROM \"DailySummaryLog\" WHERE summary_type = 'assignment' AND summary_date = :date")
                .bind("date", date)
                .map((row, metadata) -> {
                    Number count = (Number) row.get("cnt");
                    return count != null && count.longValue() > 0;
                })
                .first()
                .onErrorResume(e -> Mono.just(false));
    }

    private Mono<Boolean> isDailySummaryLogged(String userId, LocalDate date) {
        if (databaseClient == null || userId == null) return Mono.just(false);
        return databaseClient.sql("SELECT 1 FROM \"DailySummaryLog\" WHERE user_id = :userId AND summary_type = 'daily' AND summary_date = :date")
                .bind("userId", userId)
                .bind("date", date)
                .fetch()
                .first()
                .map(row -> true)
                .defaultIfEmpty(false)
                .onErrorResume(e -> Mono.just(false));
    }

    private Mono<Boolean> isAssignmentSummaryLogged(String userId, LocalDate date) {
        if (databaseClient == null || userId == null) return Mono.just(false);
        return databaseClient.sql("SELECT 1 FROM \"DailySummaryLog\" WHERE user_id = :userId AND summary_type = 'assignment' AND summary_date = :date")
                .bind("userId", userId)
                .bind("date", date)
                .fetch()
                .first()
                .map(row -> true)
                .defaultIfEmpty(false)
                .onErrorResume(e -> Mono.just(false));
    }

    private void recordDailySummaryLog(String userId, LocalDate date, String summaryType) {
        if (databaseClient == null || userId == null) return;
        databaseClient.sql("INSERT INTO \"DailySummaryLog\" (user_id, summary_type, summary_date, sent_at) VALUES (:userId, :type, :date, NOW()) ON CONFLICT DO NOTHING")
                .bind("userId", userId)
                .bind("type", summaryType)
                .bind("date", date)
                .fetch()
                .rowsUpdated()
                .onErrorResume(e -> Mono.empty())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private void updateEventReminderSentAt(Long eventId) {
        if (databaseClient == null || eventId == null) return;
        databaseClient.sql("UPDATE \"EventInfo\" SET last_reminder_sent_at = NOW() WHERE event_id = :eventId")
                .bind("eventId", eventId)
                .fetch()
                .rowsUpdated()
                .onErrorResume(e -> Mono.empty())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    // ================= DEDUP HELPERS =================
    private boolean isAlreadySent(String key, long ttlMs) {
        long now = System.currentTimeMillis();
        Long last = eventDedup.get(key);
        return (last != null && (now - last) < ttlMs);
    }

    private boolean recordSentIfAbsent(String key, long ttlMs) {
        long now = System.currentTimeMillis();
        final boolean[] updated = new boolean[]{false};
        eventDedup.compute(key, (k, last) -> {
            if (last != null && (now - last) < ttlMs) {
                return last;
            }
            updated[0] = true;
            return now;
        });
        return updated[0];
    }

    private boolean dedupCheck(String key, long ttlMs) {
        return recordSentIfAbsent(key, ttlMs);
    }

    // ================= EVENT REMINDER =================
    @Scheduled(fixedRate = 60 * 1000)
    public void checkUpcomingEvents() {
        sendEventReminders().subscribe();
    }

    public Mono<Integer> sendEventReminders() {
        Instant now = Instant.now();

        return eventInfoRepo.findEventsForReminderEvaluation(now)
                .filter(event -> {
                    if (event.getStartTime() == null) return false;
                    if ("cancelled".equalsIgnoreCase(event.getAction())) return false;
                    if (Boolean.FALSE.equals(event.getReminderEnabled())) return false;

                    int offsetMins = event.getReminderOffset() != null ? event.getReminderOffset() : 60;
                    Instant reminderTargetTime = event.getStartTime().minus(offsetMins, ChronoUnit.MINUTES);
                    // B1: Fire at or after target time; before event starts; within 15 min window
                    return !now.isBefore(reminderTargetTime)
                            && !now.isAfter(event.getStartTime())
                            && now.isBefore(reminderTargetTime.plus(15, ChronoUnit.MINUTES));
                })
                .flatMap(event -> {
                    int offset = event.getReminderOffset() != null ? event.getReminderOffset() : 60;
                    long realMinutesRemaining = Duration.between(now, event.getStartTime()).toMinutes();
                    int minutesBefore = (int) Math.max(1, realMinutesRemaining);

                    // B4: Key includes start time epoch millis so rescheduled events can remind again
                    long startEpoch = event.getStartTime() != null ? event.getStartTime().toEpochMilli() : 0L;
                    String k = eventKey(event.getUserId(), event.getEventId(), offset + "M:" + startEpoch);
                    if (isAlreadySent(k, 24 * 60 * 60 * 1000L)) return Mono.empty();

                    String startTimeStr = event.getStartTime() != null ? event.getStartTime().toString() : "";
                    String eventIdStr = String.valueOf(event.getEventId());
                    String eventTitle = event.getEventTitle() != null ? event.getEventTitle() : "Upcoming Event";
                    String eventChatId = event.getChatId() != null ? event.getChatId() : "";

                    Mono<String> chatNameMono = (eventChatId != null && !eventChatId.isEmpty() && !"0".equals(eventChatId))
                            ? userChatInfoRepo.findChatNameByChatIdAndUserId(eventChatId, event.getUserId()).defaultIfEmpty("LetsChat Event")
                            : Mono.just("LetsChat Event");

                    return chatNameMono.flatMap(chatName -> {
                        String reminderTitle = eventTitle + " starts in " + minutesBefore + " min";
                        Map<String, String> data = PushPayloadBuilder.buildEventReminderData(
                                eventIdStr,
                                eventTitle,
                                eventChatId,
                                chatName,
                                startTimeStr,
                                minutesBefore,
                                event.getLocation(),
                                event.getMeetingType(),
                                event.getMeetingLink(),
                                reminderTitle
                        );

                        return getTokens(event.getUserId())
                                .collectList()
                                .flatMap(tokens -> {
                                    if (tokens == null || tokens.isEmpty()) return Mono.empty();
                                    if (!recordSentIfAbsent(k, 24 * 60 * 60 * 1000L)) return Mono.empty();
                                    for (String token : tokens) {
                                        Mono.fromRunnable(() -> {
                                            Message message = PushPayloadBuilder.buildEventReminderMessage(data, token);
                                            safeSend(event.getUserId(), token, message, "EVENT_REMINDER");
                                        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
                                    }
                                    updateEventReminderSentAt(event.getEventId());
                                    return Mono.just(1);
                                });
                    });
                })
                .reduce(0, Integer::sum)
                .doOnError(e -> logger.error("[EVENT REMINDER] Error: {}", e.getMessage()))
                .onErrorResume(e -> Mono.just(0));
    }

    // ================= DAILY SUMMARY 7AM IST =================
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Kolkata")
    public void dailySummary() {
        sendDailySummary(null, false).subscribe();
    }

    public Mono<Integer> sendDailySummary(String targetUserId, boolean ignoreDedup) {
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(istZone);
        Instant start = today.atStartOfDay(istZone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(istZone).toInstant();

        return eventInfoRepo.findTodayEvents(start, end)
                .filter(ev -> targetUserId == null || targetUserId.equals(ev.getUserId()))
                .collectList()
                .flatMapMany(eventList -> {
                    if (eventList == null || eventList.isEmpty()) return Flux.empty();
                    Map<String, List<EventInfo>> grouped = eventList.stream()
                            .filter(ev -> ev.getUserId() != null)
                            .collect(Collectors.groupingBy(EventInfo::getUserId));
                    return Flux.fromIterable(grouped.entrySet());
                })
                .flatMap(entry -> {
                    String userId = entry.getKey();
                    List<EventInfo> eventList = entry.getValue();
                    if (eventList == null || eventList.isEmpty()) {
                        return Mono.empty();
                    }

                    String k = userId + ":daily:" + today;
                    if (!ignoreDedup && isAlreadySent(k, 24 * 60 * 60 * 1000L)) {
                        return Mono.empty();
                    }

                    // De-duplicate by groupEventId when present
                    Map<String, EventInfo> uniqueMap = new LinkedHashMap<>();
                    for (EventInfo ev : eventList) {
                        String key = (ev.getGroupEventId() != null && !ev.getGroupEventId().isEmpty())
                                ? ev.getGroupEventId()
                                : String.valueOf(ev.getEventId());
                        uniqueMap.putIfAbsent(key, ev);
                    }

                    List<EventInfo> sortedEvents = new ArrayList<>(uniqueMap.values());
                    sortedEvents.sort((a, b) -> {
                        if (a.getStartTime() == null && b.getStartTime() == null) return 0;
                        if (a.getStartTime() == null) return -1;
                        if (b.getStartTime() == null) return 1;
                        return a.getStartTime().compareTo(b.getStartTime());
                    });

                    List<Long> legacyIds = new ArrayList<>();
                    List<Map<String, String>> eventsV2List = new ArrayList<>();

                    for (EventInfo ev : sortedEvents) {
                        legacyIds.add(ev.getEventId());
                        if (eventsV2List.size() < 5) {
                            Map<String, String> item = new LinkedHashMap<>();
                            item.put("id", String.valueOf(ev.getEventId()));
                            String title = ev.getEventTitle() != null ? ev.getEventTitle() : "Event";
                            if (title.length() > 40) title = title.substring(0, 39) + "…";
                            item.put("title", title);
                            item.put("startTime", ev.getStartTime() != null ? ev.getStartTime().toString() : "");
                            item.put("location", ev.getLocation() != null ? ev.getLocation() : "");
                            eventsV2List.add(item);
                        }
                    }

                    Map<String, String> data = PushPayloadBuilder.buildDailySummaryData(
                            sortedEvents.size(),
                            eventsV2List,
                            legacyIds,
                            today.toString()
                    );

                    return getTokens(userId)
                            .collectList()
                            .flatMap(tokens -> {
                                if (tokens == null || tokens.isEmpty()) return Mono.empty();
                                if (!ignoreDedup && !recordSentIfAbsent(k, 24 * 60 * 60 * 1000L)) {
                                    return Mono.empty();
                                }
                                for (String token : tokens) {
                                    Mono.fromRunnable(() -> {
                                        Message message = PushPayloadBuilder.buildDailySummaryMessage(data, token);
                                        safeSend(userId, token, message, "DAILY");
                                    }).subscribeOn(Schedulers.boundedElastic()).subscribe();
                                }
                                recordDailySummaryLog(userId, today, "daily");
                                return Mono.just(1);
                            });
                }, 16)
                .reduce(0, Integer::sum)
                .doOnError(e -> logger.error("[DAILY SUMMARY] Error: {}", e.getMessage()))
                .onErrorResume(e -> Mono.just(0));
    }

    // ================= ASSIGNMENT MISSING 7:15 AM IST =================
    @Scheduled(cron = "0 15 7 * * *", zone = "Asia/Kolkata")
    public void assignmentMissingSummary() {
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(istZone);

        assignmentRepo.findMissingAssignments()
                .flatMap(dto -> {
                    if (dto.getAssignmentIds() == null || dto.getAssignmentIds().isEmpty()) {
                        return Mono.empty();
                    }

                    String k = dto.getUserId() + ":assignment:" + today;
                    if (isAlreadySent(k, 24 * 60 * 60 * 1000L)) {
                        return Mono.empty();
                    }

                    List<Long> ids = dto.getAssignmentIds();

                    return assignmentRepo.findMissingAssignmentDetails(ids, dto.getUserId())
                            .collectList()
                            .flatMap(details -> {
                                List<Map<String, String>> assignmentsV2List = new ArrayList<>();
                                for (MissingAssignmentItemDTO item : details) {
                                    if (assignmentsV2List.size() < 5) {
                                        Map<String, String> map = new LinkedHashMap<>();
                                        map.put("id", String.valueOf(item.getAssignmentId()));
                                        String title = item.getTitle() != null ? item.getTitle() : "Assignment";
                                        if (title.length() > 40) title = title.substring(0, 39) + "…";
                                        map.put("title", title);
                                        map.put("deadline", item.getDeadline() != null ? item.getDeadline().toString() : "");
                                        map.put("chatName", item.getChatName() != null ? item.getChatName() : "");
                                        map.put("chatId", item.getChatId() != null ? item.getChatId() : "");
                                        assignmentsV2List.add(map);
                                    }
                                }

                                Map<String, String> data = PushPayloadBuilder.buildMissingAssignmentData(
                                        dto.getUserId(),
                                        ids.size(),
                                        assignmentsV2List,
                                        ids
                                );

                                return getTokens(dto.getUserId())
                                        .collectList()
                                        .flatMap(tokens -> {
                                            if (tokens == null || tokens.isEmpty()) return Mono.empty();
                                            if (!recordSentIfAbsent(k, 24 * 60 * 60 * 1000L)) return Mono.empty();
                                            for (String token : tokens) {
                                                Mono.fromRunnable(() -> {
                                                    Message message = PushPayloadBuilder.buildMissingAssignmentMessage(data, token);
                                                    safeSend(dto.getUserId(), token, message, "ASSIGNMENT_MISSING");
                                                }).subscribeOn(Schedulers.boundedElastic()).subscribe();
                                            }
                                            recordDailySummaryLog(dto.getUserId(), today, "assignment");
                                            return Mono.just(1);
                                        });
                            });
                }, 16)
                .reduce(0, Integer::sum)
                .doOnError(e -> logger.error("[ASSIGNMENT SUMMARY] Error: {}", e.getMessage()))
                .onErrorResume(e -> Mono.just(0))
                .subscribe();
    }

    // ================= EVENT CHANGED PUSH =================

    public void sendEventChange(String userId, String eventId, String title, String changeSummary, String startTime) {

        sendGroupEventNotification(userId, "", "", eventId, title, changeSummary, startTime);

    }



    public void sendGroupEventNotification(String userId, String chatId, String groupName, String eventId, String title, String summary, String startTime) {

        sendGroupEventNotification(userId, chatId, groupName, eventId, title, summary, startTime, "updated", Collections.emptyList(), startTime, "", "");

    }



    public void sendGroupEventNotification(

            String userId, String chatId, String groupName, String eventId,

            String title, String summary, String startTime, String action,

            List<Map<String, String>> changesList, String effectiveStartTime,

            String location, String personName

    ) {

        if (userId == null || userId.isEmpty()) return;

        Map<String, String> data = PushPayloadBuilder.buildEventChangedData(

                action != null ? action : "updated",

                eventId,

                title,

                chatId,

                groupName,

                effectiveStartTime != null ? effectiveStartTime : startTime,

                changesList,

                personName != null ? personName : "",

                location != null ? location : "",

                groupName != null ? groupName : "Event Update",

                summary != null ? summary : "Event details updated."

        );



        getTokens(userId)

                .flatMap(token ->

                        Mono.fromRunnable(() -> {

                            Message message = PushPayloadBuilder.buildEventChangedMessage(data, token);

                            safeSend(userId, token, message, "EVENT_CHANGED");

                        }).subscribeOn(Schedulers.boundedElastic())

                )

                .subscribe(null, e -> logger.error("[EVENT_NOTIF] Subscription error for user {}: {}", userId, e.getMessage()));

    }



    // ================= SAFE SEND WITH UNIFIED TOKEN CLEANUP =================

            private void flushChatStateDirect(String k, ChatState state) {
        if (state == null) return;
        int unreadToSend;
        int batchCountToSend;
        List<Map<String, String>> itemsToSend;
        List<String> legacyMsgsToSend;
        String finalSenderId;
        String finalSenderName;
        String finalMsgType;
        String finalMsgId;
        String finalSpaceId;
        String finalPreview;
        long finalTs;

        synchronized (state) {
            if (state.batchCount == 0) {
                state.firstPendingAt = 0;
                return;
            }
            unreadToSend = state.unread;
            batchCountToSend = state.batchCount;
            itemsToSend = new ArrayList<>(state.items);
            legacyMsgsToSend = new ArrayList<>(state.messages);
            finalSenderId = state.lastSenderId;
            finalSenderName = state.lastSenderName;
            finalMsgType = state.lastMsgType;
            finalMsgId = state.lastMsgId;
            finalSpaceId = state.lastSpaceId;
            finalPreview = state.lastPreview;
            finalTs = state.lastTs;

            state.batchCount = 0;
            state.firstPendingAt = 0;
        }

        String[] parts = k.split(":", 2);
        String userId = parts[0];
        String chatId = parts.length > 1 ? parts[1] : "";

        Map<String, String> data = PushPayloadBuilder.buildChatData(
                chatId,
                "New Messages",
                "Chat",
                "group",
                "message",
                finalSenderId,
                finalSenderName,
                finalMsgType,
                finalMsgId,
                finalSpaceId,
                finalPreview,
                itemsToSend,
                batchCountToSend,
                unreadToSend,
                finalTs,
                false,
                false,
                legacyMsgsToSend,
                "group",
                null
        );

        getTokens(userId)
                .collectList()
                .subscribe(tokens -> {
                    if (tokens != null && !tokens.isEmpty()) {
                        for (String token : tokens) {
                            Message message = PushPayloadBuilder.buildChatMessage(data, token);
                            safeSend(userId, token, message, "CHAT_SHUTDOWN_FLUSH");
                        }
                    }
                }, err -> logger.error("[SHUTDOWN] Error flushing tokens for user {}: {}", userId, err.getMessage()));
    }

    @PreDestroy
    public void shutdown() {
        try {
            logger.info("[PUSH] Shutting down notification scheduler and flushing pending debounces...");
            for (Map.Entry<String, ScheduledFuture<?>> entry : timers.entrySet()) {
                ScheduledFuture<?> timer = entry.getValue();
                if (timer != null) {
                    timer.cancel(false);
                }
                String k = entry.getKey();
                ChatState state = store.get(k);
                if (state != null) {
                    flushChatStateDirect(k, state);
                }
            }
            timers.clear();
        } catch (Exception e) {
            logger.error("[NOTIFICATION] Error flushing pending states during shutdown: {}", e.getMessage());
        }

        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        ZonedDateTime nowIst = ZonedDateTime.now(istZone);
        int hour = nowIst.getHour();
        if (hour >= 7 && hour < 24) {
            String dailyDedupKey = "SYSTEM:startup_catchup_daily:" + nowIst.toLocalDate();
            checkIfDailySummarySentToday(nowIst.toLocalDate())
                .flatMap(alreadySent -> {
                    if (Boolean.TRUE.equals(alreadySent)) {
                        logger.info("[STARTUP] Daily summary was already sent today for date {}. Skipping startup catch-up.", nowIst.toLocalDate());
                        return Mono.empty();
                    }
                    if (recordSentIfAbsent(dailyDedupKey, 24 * 60 * 60 * 1000L)) {
                        logger.info("[STARTUP] Checking catch-up for daily summary for date {}", nowIst.toLocalDate());
                        dailySummary();
                    }
                    return Mono.empty();
                })
                .subscribe();

            if (hour >= 7 && (hour > 7 || nowIst.getMinute() >= 15)) {
                String assignDedupKey = "SYSTEM:startup_catchup_assign:" + nowIst.toLocalDate();
                checkIfAssignmentSummarySentToday(nowIst.toLocalDate())
                    .flatMap(alreadySent -> {
                        if (Boolean.TRUE.equals(alreadySent)) {
                            logger.info("[STARTUP] Assignment summary was already sent today for date {}. Skipping startup catch-up.", nowIst.toLocalDate());
                            return Mono.empty();
                        }
                        if (recordSentIfAbsent(assignDedupKey, 24 * 60 * 60 * 1000L)) {
                            logger.info("[STARTUP] Checking catch-up for assignment missing summary for date {}", nowIst.toLocalDate());
                            assignmentMissingSummary();
                        }
                        return Mono.empty();
                    })
                    .subscribe();
            }
        }
    }

    private void safeSend(String userId, String token, Message message, String typeForLog) {
        scheduler.execute(() -> safeSendWithRetry(userId, token, message, typeForLog, true));
    }

    private void safeSendWithRetry(String userId, String token, Message message, String typeForLog, boolean allowRetry) {
        try {
            FirebaseMessaging.getInstance().send(message);
            logger.info("[PUSH] Sent {} to user {}", typeForLog, userId);
        } catch (FirebaseMessagingException e) {
            String errorCode = e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "";
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

            if ("UNREGISTERED".equalsIgnoreCase(errorCode)) {
                logger.warn("[PUSH] Unregistered token for user {}. Deleting token.", userId);
                fcmRepo.deleteByToken(token)
                        .subscribeOn(Schedulers.boundedElastic())
                        .doOnError(err -> logger.error("[PUSH] Error deleting unregistered token", err))
                        .subscribe();
                removeToken(userId, token);
            } else if ("INVALID_ARGUMENT".equalsIgnoreCase(errorCode)) {
                boolean isBadToken = msg.contains("registration-token")
                        || msg.contains("registration token")
                        || msg.contains("invalid registration token")
                        || msg.contains("malformed registration token");
                if (isBadToken) {
                    logger.warn("[PUSH] Invalid registration token for user {}. Deleting token.", userId);
                    fcmRepo.deleteByToken(token)
                            .subscribeOn(Schedulers.boundedElastic())
                            .doOnError(err -> logger.error("[PUSH] Error deleting invalid token", err))
                            .subscribe();
                    removeToken(userId, token);
                } else {
                    logger.error("[PUSH] INVALID_ARGUMENT payload/size error for user {}: {}", userId, e.getMessage());
                }
            } else if ("SENDER_ID_MISMATCH".equalsIgnoreCase(errorCode)) {
                logger.warn("[PUSH] Sender ID mismatch for user {}. Deleting token.", userId);
                fcmRepo.deleteByToken(token)
                        .subscribeOn(Schedulers.boundedElastic())
                        .doOnError(err -> logger.error("[PUSH] Error deleting mismatched token", err))
                        .subscribe();
                removeToken(userId, token);
            } else if ("QUOTA_EXCEEDED".equalsIgnoreCase(errorCode)) {
                logger.warn("[PUSH] FCM Quota exceeded for user {}. Scheduling retry...", userId);
                if (allowRetry) {
                    scheduler.schedule(() -> {
                        safeSendWithRetry(userId, token, message, typeForLog, false);
                    }, 2, TimeUnit.SECONDS);
                }
            } else if ("THIRD_PARTY_AUTH_ERROR".equalsIgnoreCase(errorCode) || "UNAUTHENTICATED".equalsIgnoreCase(errorCode)) {
                logger.error("[PUSH] FCM Authentication error for user {}: {}", userId, e.getMessage());
            } else if (allowRetry && ("UNAVAILABLE".equalsIgnoreCase(errorCode) || "INTERNAL".equalsIgnoreCase(errorCode))) {
                logger.warn("[PUSH] {} error for user {}. Retrying in 1s...", errorCode, userId);
                scheduler.schedule(() -> {
                    safeSendWithRetry(userId, token, message, typeForLog, false);
                }, 1, TimeUnit.SECONDS);
            } else {
                logger.error("[PUSH] Failed sending {} to user {}: error code {}", typeForLog, userId, errorCode);
            }
        } catch (Exception e) {
            logger.error("[PUSH] Unexpected error sending {} to user {}: {}", typeForLog, userId, e.getMessage());
        }
    }
}
