package com.letschat.mvp_1;

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

    public NotificationService(FCMRepo fcmRepo, EventInfoRepo eventInfoRepo, UserChatInfoRepo userChatInfoRepo, AssignmentRepo assignmentRepo) {
        this.fcmRepo = fcmRepo;
        this.eventInfoRepo = eventInfoRepo;
        this.userChatInfoRepo = userChatInfoRepo;
        this.assignmentRepo = assignmentRepo;
    }

    // ================= CHAT STATE =================
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

    public void invalidateTokenCache(String userId) {
        if (userId != null) {
            tokenCache.remove(userId);
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
            if (state.firstPendingAt <= 0) {
                state.firstPendingAt = System.currentTimeMillis();
            }
            state.messages.add(message);
            if (state.messages.size() > MAX_MESSAGES) {
                state.messages.remove(0);
            }

            if (alreadyAnnounced) {
                if (msgId != null && !msgId.isEmpty()) {
                    state.announcedMsgIds.add(msgId);
                }
            } else {
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

        ScheduledFuture<?> existing = timers.get(k);
        if (existing != null) existing.cancel(false);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
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
                    state.items.clear();
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
                        .subscribe(tokens -> {
                            if (tokens != null && !tokens.isEmpty()) {
                                for (String token : tokens) {
                                    Message message = PushPayloadBuilder.buildChatMessage(data, token);
                                    safeSend(userId, token, message, "CHAT");
                                }
                            }
                        });

                timers.remove(k);
            }).subscribeOn(Schedulers.boundedElastic()).subscribe();
        }, delayMs, TimeUnit.MILLISECONDS);

        timers.put(k, future);
    }

    // ================= DEDUP HELPER =================
    private boolean dedupCheck(String key, long ttlMs) {
        long now = System.currentTimeMillis();
        Long last = eventDedup.get(key);
        if (last != null && (now - last) < ttlMs) return false;
        eventDedup.put(key, now);
        return true;
    }

    // ================= EVENT REMINDER =================
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void checkUpcomingEvents() {
        Instant now = Instant.now();
        Instant maxFuture = now.plus(25, ChronoUnit.HOURS);

        eventInfoRepo.findEventsForReminderEvaluation(now, maxFuture)
                .filter(event -> {
                    if (event.getStartTime() == null) return false;
                    if ("cancelled".equalsIgnoreCase(event.getAction())) return false;
                    if (Boolean.FALSE.equals(event.getReminderEnabled())) return false;

                    int offsetMins = event.getReminderOffset() != null ? event.getReminderOffset() : 60;
                    Instant reminderTargetTime = event.getStartTime().minus(offsetMins, ChronoUnit.MINUTES);
                    long diffSeconds = ChronoUnit.SECONDS.between(now, reminderTargetTime);
                    return diffSeconds >= -600 && diffSeconds <= 600;
                })
                .flatMap(event -> {
                    int offset = event.getReminderOffset() != null ? event.getReminderOffset() : 60;
                    // 24-hour in-memory dedup key as requested
                    String k = eventKey(event.getUserId(), event.getEventId(), offset + "M");
                    if (!dedupCheck(k, 24 * 60 * 60 * 1000L)) return Mono.empty();

                    String startTimeStr = event.getStartTime() != null ? event.getStartTime().toString() : "";
                    String eventIdStr = String.valueOf(event.getEventId());
                    String eventTitle = event.getEventTitle() != null ? event.getEventTitle() : "Upcoming Event";
                    String eventChatId = event.getChatId() != null ? event.getChatId() : "";

                    Mono<String> chatNameMono = (eventChatId != null && !eventChatId.isEmpty() && !"0".equals(eventChatId))
                            ? userChatInfoRepo.findChatNameByChatIdAndUserId(eventChatId, event.getUserId()).defaultIfEmpty("LetsChat Event")
                            : Mono.just("LetsChat Event");

                    return chatNameMono.flatMap(chatName -> {
                        Map<String, String> data = PushPayloadBuilder.buildEventReminderData(
                                eventIdStr,
                                eventTitle,
                                eventChatId,
                                chatName,
                                startTimeStr,
                                offset,
                                event.getLocation(),
                                event.getMeetingType(),
                                event.getMeetingLink(),
                                eventTitle
                        );

                        return getTokens(event.getUserId())
                                .flatMap(token ->
                                        Mono.fromRunnable(() -> {
                                            Message message = PushPayloadBuilder.buildEventReminderMessage(data, token);
                                            safeSend(event.getUserId(), token, message, "EVENT_REMINDER");
                                        }).subscribeOn(Schedulers.boundedElastic())
                                )
                                .then();
                    });
                })
                .doOnError(e -> logger.error("[EVENT REMINDER] Error: {}", e.getMessage()))
                .onErrorResume(e -> Flux.empty())
                .subscribe();
    }

    // ================= DAILY SUMMARY 7AM IST =================
    @Scheduled(cron = "0 0 7 * * *", zone = "Asia/Kolkata")
    public void dailySummary() {
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(istZone);
        Instant start = today.atStartOfDay(istZone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(istZone).toInstant();

        eventInfoRepo.findTodayEvents(start, end)
                .groupBy(e -> e.getUserId())
                .flatMap(group -> group
                        .collectList()
                        .flatMap(eventList -> {
                            String userId = group.key();
                            if (eventList == null || eventList.isEmpty()) {
                                return Mono.empty();
                            }

                            String k = userId + ":daily:" + today;
                            if (!dedupCheck(k, 24 * 60 * 60 * 1000L))
                                return Mono.empty();

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
                                    .flatMap(token ->
                                            Mono.fromRunnable(() -> {
                                                Message message = PushPayloadBuilder.buildDailySummaryMessage(data, token);
                                                safeSend(userId, token, message, "DAILY");
                                            }).subscribeOn(Schedulers.boundedElastic())
                                    )
                                    .then();
                        })
                )
                .doOnError(e -> logger.error("[DAILY SUMMARY] Error: {}", e.getMessage()))
                .onErrorResume(e -> Flux.empty())
                .subscribe();
    }

    // ================= ASSIGNMENT MISSING 7AM IST =================
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
                                        .flatMap(token ->
                                                Mono.fromRunnable(() -> {
                                                    Message message = PushPayloadBuilder.buildMissingAssignmentMessage(data, token);
                                                    safeSend(dto.getUserId(), token, message, "ASSIGNMENT_MISSING");
                                                }).subscribeOn(Schedulers.boundedElastic())
                                        )
                                        .then();
                            });
                })
                .doOnError(e -> logger.error("[ASSIGNMENT SUMMARY] Error: {}", e.getMessage()))
                .onErrorResume(e -> Flux.empty())
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
    private void safeSend(String userId, String token, Message message, String typeForLog) {
        safeSendWithRetry(userId, token, message, typeForLog, true);
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
                        .doOnError(err -> logger.error("[PUSH] Error deleting unregistered token", err))
                        .subscribe();
                removeToken(userId, token);
            } else if ("INVALID_ARGUMENT".equalsIgnoreCase(errorCode)) {
                if (msg.contains("registration-token") || msg.contains("registration token") || msg.contains("token")) {
                    logger.warn("[PUSH] Invalid registration token for user {}. Deleting token.", userId);
                    fcmRepo.deleteByToken(token)
                            .doOnError(err -> logger.error("[PUSH] Error deleting invalid token", err))
                            .subscribe();
                    removeToken(userId, token);
                } else {
                    logger.error("[PUSH] INVALID_ARGUMENT payload error for user {}: {}", userId, e.getMessage());
                }
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
