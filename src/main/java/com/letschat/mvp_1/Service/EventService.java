package com.letschat.mvp_1.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.letschat.mvp_1.NotificationService;
import com.letschat.mvp_1.DTOs.*;
import com.letschat.mvp_1.Models.EventInfo;
import com.letschat.mvp_1.Models.UserInfo;
import com.letschat.mvp_1.Repositories.EventInfoRepo;
import com.letschat.mvp_1.Repositories.GroupInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;
import com.letschat.mvp_1.Repositories.UserChatInfoRepo;
import com.letschat.mvp_1.Repositories.MessageInfoRepo;
import com.letschat.mvp_1.WebSocket.MyWebSocketHandler;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class EventService {
    private final EventInfoRepo eventInfoRepo;
    private final GroupInfoRepo groupInfoRepo;
    private final UserInfoRepo userInfoRepo;
    private final MyWebSocketHandler myWebSocketHandler;
    private final BrevoMailService brevoMailService;
    private final NotificationService notificationService;
    private final UserChatInfoRepo userChatInfoRepo;
    private final MessageInfoRepo messageInfoRepo;

    @Value("${letschat.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${letschat.app.download-url:https://letschat.com/download}")
    private String appDownloadUrl;

    public EventService(
        EventInfoRepo eventInfoRepo,
        GroupInfoRepo groupInfoRepo,
        UserInfoRepo userInfoRepo,
        MyWebSocketHandler myWebSocketHandler,
        BrevoMailService brevoMailService,
        NotificationService notificationService,
        UserChatInfoRepo userChatInfoRepo,
        MessageInfoRepo messageInfoRepo
    ) {
        this.eventInfoRepo = eventInfoRepo;
        this.groupInfoRepo = groupInfoRepo;
        this.userInfoRepo = userInfoRepo;
        this.myWebSocketHandler = myWebSocketHandler;
        this.brevoMailService = brevoMailService;
        this.notificationService = notificationService;
        this.userChatInfoRepo = userChatInfoRepo;
        this.messageInfoRepo = messageInfoRepo;
    }

    // Legacy support
    public Mono<Long> addevent(EventInfo event, String userId) {
        event.setUserId(userId);
        if (event.getReminderOffset() == null) {
            event.setReminderOffset(60);
        }
        return eventInfoRepo.save(event).map(EventInfo::getEventId);
    }

    public Flux<EventInfo> getevents(String userId) {
        return eventInfoRepo.findByUserId(userId);
    }

    public Mono<String> action(EventInfo event) {
        if (event.getEventId() != null) {
            return eventInfoRepo.findById(event.getEventId())
                .flatMap(existing -> {
                    String creatorId = existing.getCreatorId();
                    if (creatorId != null && !creatorId.trim().isEmpty() && !creatorId.equals(event.getUserId())) {
                        // Receiving attendee: only allow updating personal flags, keep creator's event details intact
                        if (event.getIsAdded() != null) existing.setIsAdded(event.getIsAdded());
                        if (event.getIsSync() != null) existing.setIsSync(event.getIsSync());
                        if (event.getReminderEnabled() != null) existing.setReminderEnabled(event.getReminderEnabled());
                        if (event.getReminderOffset() != null) existing.setReminderOffset(event.getReminderOffset());
                        return eventInfoRepo.save(existing).thenReturn("ok");
                    }
                    return eventInfoRepo.save(event).thenReturn("ok");
                })
                .switchIfEmpty(Mono.defer(() -> eventInfoRepo.save(event).thenReturn("ok")));
        }
        return eventInfoRepo.save(event).thenReturn("ok");
    }

    private void triggerEventBannerAndNotification(String chatId, String userId, String groupEventId, String eventTitle, String action, Instant start) {
        if (chatId == null || chatId.isEmpty() || userId == null || userId.isEmpty()) return;

        userInfoRepo.findById(userId)
            .flatMap(user -> {
                String rawUserName = user.getUsername() != null ? user.getUsername() : user.getPublicName();
                final String userName = (rawUserName != null && !rawUserName.trim().isEmpty()) ? rawUserName.trim() : "User";
                final String safeTitle = (eventTitle != null && !eventTitle.trim().isEmpty()) ? eventTitle.trim() : "Event";
                final String actionLower = (action != null) ? action.trim().toLowerCase() : "scheduled";

                String othersText;
                String youText;

                if ("cancelled".equals(actionLower)) {
                    othersText = userName + " cancelled " + safeTitle;
                    youText = "You cancelled " + safeTitle;
                } else if ("postponed".equals(actionLower) || "rescheduled".equals(actionLower)) {
                    othersText = userName + " postponed " + safeTitle;
                    youText = "You postponed " + safeTitle;
                } else {
                    String firstChar = safeTitle.isEmpty() ? "" : safeTitle.substring(0, 1).toLowerCase();
                    String article = "aeiou".indexOf(firstChar) >= 0 ? "an" : "a";
                    othersText = userName + " scheduled " + article + " " + safeTitle;
                    youText = "You scheduled " + article + " " + safeTitle;
                }

                String fullContent = othersText + "/" + youText;
                String bannerMsgId = "Msg-" + UUID.randomUUID().toString();
                java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));

                return messageInfoRepo.insert(
                    bannerMsgId,
                    chatId,
                    userId,
                    "event-banner-pin",
                    fullContent,
                    groupEventId,
                    null,
                    now,
                    0
                ).flatMap(savedMsg -> {
                    return myWebSocketHandler.broadcastBannerPin(
                        chatId,
                        bannerMsgId,
                        userId,
                        othersText,
                        youText,
                        "event-banner-pin",
                        groupEventId,
                        now
                    );
                }).then(
                    userChatInfoRepo.findChatNameByChatId(chatId)
                        .defaultIfEmpty("Group")
                        .flatMap(groupName -> {
                            String notifSummary = "cancelled".equals(actionLower)
                                ? (userName + " cancelled \"" + safeTitle + "\" in " + groupName)
                                : ("postponed".equals(actionLower) || "rescheduled".equals(actionLower))
                                    ? (userName + " postponed \"" + safeTitle + "\" in " + groupName)
                                    : (userName + " scheduled \"" + safeTitle + "\" in " + groupName);

                            String startStr = (start != null) ? start.toString() : "";

                            return userChatInfoRepo.findUserIds(chatId)
                                .filter(mId -> !mId.equals(userId))
                                .doOnNext(memberId -> {
                                    notificationService.sendGroupEventNotification(
                                        memberId,
                                        chatId,
                                        groupName,
                                        groupEventId,
                                        safeTitle,
                                        notifSummary,
                                        startStr
                                    );
                                })
                                .then();
                        })
                );
            })
            .subscribe(null, err -> System.err.println("[EventService] Error triggering banner/notif: " + err.getMessage()));
    }

    // ================= GROUP EVENT CREATION & BATCH SYNC =================
    public Mono<EventInfo> createGroupEvent(GroupEventCreateDTO dto, String userId) {
        String groupEventId = (dto.getGroupEventId() != null && !dto.getGroupEventId().isEmpty()) 
            ? dto.getGroupEventId() 
            : UUID.randomUUID().toString();

        Instant start = dto.getStartTime() != null ? dto.getStartTime() : Instant.now();
        Instant end = dto.getEndTime() != null ? dto.getEndTime() : start.plus(1, ChronoUnit.HOURS);
        Integer reminderOffset = dto.getReminderOffset() != null ? dto.getReminderOffset() : 60;

        // Scope "ALL" within a group: batch insert for all group members
        if ("ALL".equalsIgnoreCase(dto.getScope()) && dto.getChatId() != null && !dto.getChatId().isEmpty()) {
            return groupInfoRepo.getMembers(dto.getChatId())
                .map(member -> member.getUserId())
                .collectList()
                .flatMap(memberIds -> {
                    Set<String> uniqueMembers = new HashSet<>(memberIds);
                    uniqueMembers.add(userId); // ensure creator is included

                    List<EventInfo> batch = new ArrayList<>();
                    EventInfo creatorEvent = null;

                    for (String memberId : uniqueMembers) {
                        EventInfo ev = new EventInfo();
                        ev.setUserId(memberId);
                        ev.setGroupEventId(groupEventId);
                        ev.setCreatorId(userId);
                        ev.setEventTitle(dto.getEventTitle());
                        ev.setStartTime(start);
                        ev.setEndTime(end);
                        ev.setEventDetails(dto.getEventDetails());
                        ev.setEventColor(dto.getEventColor() != null ? dto.getEventColor() : "#3b82f6");
                        ev.setMsgId(dto.getMsgId());
                        ev.setChatId(dto.getChatId());
                        ev.setAction(dto.getAction() != null ? dto.getAction() : "scheduled");
                        ev.setScope("ALL");
                        ev.setIsAdded(memberId.equals(userId));
                        ev.setIsSync(memberId.equals(userId));
                        ev.setIsGroupVisible(dto.getIsGroupVisible());
                        ev.setMeetingType(dto.getMeetingType());
                        ev.setMeetingLink(dto.getMeetingLink());
                        ev.setLocation(dto.getLocation());
                        ev.setReminderEnabled(true);
                        ev.setReminderOffset(reminderOffset);
                        ev.setCustomReminderTime(dto.getCustomReminderTime());
                        ev.setRsvpStatus(memberId.equals(userId) ? "going" : null);

                        if (memberId.equals(userId)) {
                            creatorEvent = ev;
                        }
                        batch.add(ev);
                    }

                    final EventInfo finalCreatorEvent = creatorEvent != null ? creatorEvent : batch.get(0);

                    return eventInfoRepo.saveAll(batch)
                        .filter(saved -> saved.getUserId().equals(userId))
                        .next()
                        .defaultIfEmpty(finalCreatorEvent)
                        .doOnSuccess(saved -> {
                            // Broadcast event_sync frame to all group members over WebSocket
                            EventSyncMessageDTO syncMsg = new EventSyncMessageDTO();
                            syncMsg.setType("event_sync");
                            syncMsg.setGroupEventId(groupEventId);
                            syncMsg.setChatId(dto.getChatId());
                            syncMsg.setMsgId(dto.getMsgId());
                            syncMsg.setEventTitle(dto.getEventTitle());
                            syncMsg.setStartTime(start);
                            syncMsg.setEndTime(end);
                            syncMsg.setAction(dto.getAction() != null ? dto.getAction() : "scheduled");
                            syncMsg.setEventColor(dto.getEventColor());
                            syncMsg.setMeetingType(dto.getMeetingType());
                            syncMsg.setMeetingLink(dto.getMeetingLink());
                            syncMsg.setLocation(dto.getLocation());
                            syncMsg.setUpdatedBy(userId);

                            myWebSocketHandler.broadcastEventSync(dto.getChatId(), userId, syncMsg).subscribe();
                            triggerEventBannerAndNotification(dto.getChatId(), userId, groupEventId, dto.getEventTitle(), "scheduled", start);
                        });
                });
        }

        // If an existing group event was created by another user, receiving persons can only add it (preserving creator's details)
        if (dto.getGroupEventId() != null && !dto.getGroupEventId().isEmpty()) {
            return eventInfoRepo.findByGroupEventId(dto.getGroupEventId())
                .next()
                .flatMap(existingEvent -> {
                    String creatorId = existingEvent.getCreatorId();
                    boolean isReceiver = creatorId != null && !creatorId.isEmpty() && !creatorId.equals(userId);

                    return eventInfoRepo.findByGroupEventIdAndUserId(dto.getGroupEventId(), userId)
                        .flatMap(userRow -> {
                            // User already has a batch row: mark as added & sync without altering creator details
                            userRow.setIsAdded(true);
                            userRow.setIsSync(true);
                            userRow.setRsvpStatus("going");
                            if (isReceiver) {
                                userRow.setEventTitle(existingEvent.getEventTitle());
                                userRow.setStartTime(existingEvent.getStartTime());
                                userRow.setEndTime(existingEvent.getEndTime());
                                userRow.setEventDetails(existingEvent.getEventDetails());
                                userRow.setEventColor(existingEvent.getEventColor());
                                userRow.setMeetingType(existingEvent.getMeetingType());
                                userRow.setMeetingLink(existingEvent.getMeetingLink());
                                userRow.setLocation(existingEvent.getLocation());
                            }
                            return eventInfoRepo.save(userRow);
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            EventInfo ev = new EventInfo();
                            ev.setUserId(userId);
                            ev.setGroupEventId(groupEventId);
                            ev.setCreatorId(isReceiver ? existingEvent.getCreatorId() : userId);
                            ev.setEventTitle(isReceiver ? existingEvent.getEventTitle() : dto.getEventTitle());
                            ev.setStartTime(isReceiver ? existingEvent.getStartTime() : start);
                            ev.setEndTime(isReceiver ? existingEvent.getEndTime() : end);
                            ev.setEventDetails(isReceiver ? existingEvent.getEventDetails() : dto.getEventDetails());
                            ev.setEventColor(isReceiver ? existingEvent.getEventColor() : (dto.getEventColor() != null ? dto.getEventColor() : "#3b82f6"));
                            ev.setMsgId(isReceiver ? existingEvent.getMsgId() : dto.getMsgId());
                            ev.setChatId(isReceiver ? existingEvent.getChatId() : dto.getChatId());
                            ev.setAction(isReceiver ? existingEvent.getAction() : (dto.getAction() != null ? dto.getAction() : "scheduled"));
                            ev.setScope("PERSONAL");
                            ev.setIsAdded(true);
                            ev.setIsSync(true);
                            ev.setIsGroupVisible(existingEvent.getIsGroupVisible());
                            ev.setMeetingType(isReceiver ? existingEvent.getMeetingType() : dto.getMeetingType());
                            ev.setMeetingLink(isReceiver ? existingEvent.getMeetingLink() : dto.getMeetingLink());
                            ev.setLocation(isReceiver ? existingEvent.getLocation() : dto.getLocation());
                            ev.setReminderEnabled(true);
                            ev.setReminderOffset(reminderOffset);
                            ev.setCustomReminderTime(dto.getCustomReminderTime());
                            ev.setRsvpStatus("going");
                            return eventInfoRepo.save(ev);
                        }));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    EventInfo ev = new EventInfo();
                    ev.setUserId(userId);
                    ev.setGroupEventId(groupEventId);
                    ev.setCreatorId(userId);
                    ev.setEventTitle(dto.getEventTitle());
                    ev.setStartTime(start);
                    ev.setEndTime(end);
                    ev.setEventDetails(dto.getEventDetails());
                    ev.setEventColor(dto.getEventColor() != null ? dto.getEventColor() : "#3b82f6");
                    ev.setMsgId(dto.getMsgId());
                    ev.setChatId(dto.getChatId());
                    ev.setAction(dto.getAction() != null ? dto.getAction() : "scheduled");
                    ev.setScope("PERSONAL");
                    ev.setIsAdded(true);
                    ev.setIsSync(true);
                    ev.setIsGroupVisible(dto.getIsGroupVisible());
                    ev.setMeetingType(dto.getMeetingType());
                    ev.setMeetingLink(dto.getMeetingLink());
                    ev.setLocation(dto.getLocation());
                    ev.setReminderEnabled(true);
                    ev.setReminderOffset(reminderOffset);
                    ev.setCustomReminderTime(dto.getCustomReminderTime());
                    ev.setRsvpStatus("going");
                    return eventInfoRepo.save(ev);
                }));
        }

        // Scope "PERSONAL" or private calendar event (truly new event or detected event)
        EventInfo ev = new EventInfo();
        ev.setUserId(userId);
        ev.setGroupEventId(groupEventId);
        ev.setCreatorId(userId);
        ev.setEventTitle(dto.getEventTitle());
        ev.setStartTime(start);
        ev.setEndTime(end);
        ev.setEventDetails(dto.getEventDetails());
        ev.setEventColor(dto.getEventColor() != null ? dto.getEventColor() : "#3b82f6");
        ev.setMsgId(dto.getMsgId());
        ev.setChatId(dto.getChatId());
        ev.setAction(dto.getAction() != null ? dto.getAction() : "scheduled");
        ev.setScope("PERSONAL");
        ev.setIsAdded(true);
        ev.setIsSync(true);
        ev.setIsGroupVisible(dto.getIsGroupVisible());
        ev.setMeetingType(dto.getMeetingType());
        ev.setMeetingLink(dto.getMeetingLink());
        ev.setLocation(dto.getLocation());
        ev.setReminderEnabled(true);
        ev.setReminderOffset(reminderOffset);
        ev.setCustomReminderTime(dto.getCustomReminderTime());
        ev.setRsvpStatus("going");

        return eventInfoRepo.save(ev)
            .doOnSuccess(saved -> {
                if (dto.getChatId() != null && dto.getIsGroupVisible()) {
                    EventSyncMessageDTO syncMsg = new EventSyncMessageDTO();
                    syncMsg.setType("event_sync");
                    syncMsg.setGroupEventId(groupEventId);
                    syncMsg.setChatId(dto.getChatId());
                    syncMsg.setMsgId(dto.getMsgId());
                    syncMsg.setEventTitle(dto.getEventTitle());
                    syncMsg.setStartTime(start);
                    syncMsg.setEndTime(end);
                    syncMsg.setAction("scheduled");
                    syncMsg.setEventColor(dto.getEventColor());
                    syncMsg.setMeetingType(dto.getMeetingType());
                    syncMsg.setMeetingLink(dto.getMeetingLink());
                    syncMsg.setLocation(dto.getLocation());
                    syncMsg.setUpdatedBy(userId);

                    myWebSocketHandler.broadcastEventSync(dto.getChatId(), userId, syncMsg).subscribe();
                }
            });
    }

    // ================= GROUP EVENTS RETRIEVAL =================
    public Flux<EventInfo> getGroupEvents(String chatId, String userId) {
        return eventInfoRepo.findGroupEvents(chatId)
            .flatMap(groupEvent -> {
                String gid = groupEvent.getGroupEventId();
                Mono<EventInfo> eventMono = (gid != null && !gid.isEmpty())
                    ? eventInfoRepo.findByGroupEventIdAndUserId(gid, userId).defaultIfEmpty(groupEvent)
                    : Mono.just(groupEvent);

                return eventMono.flatMap(ev -> {
                    String effectiveCreatorId = ev.getCreatorId() != null ? ev.getCreatorId() : groupEvent.getCreatorId();
                    if (effectiveCreatorId != null && !effectiveCreatorId.isEmpty()) {
                        return userInfoRepo.findById(effectiveCreatorId)
                            .map(u -> {
                                ev.setCreatorName(u.getUsername() != null ? u.getUsername() : u.getPublicName());
                                return ev;
                            })
                            .defaultIfEmpty(ev);
                    }
                    return Mono.just(ev);
                });
            });
    }

    // ================= RESCHEDULE & CANCEL GROUP SYNC =================
    public Mono<String> rescheduleOrCancel(GroupEventCreateDTO dto, String userId) {
        if (dto.getGroupEventId() != null && !dto.getGroupEventId().isEmpty()) {
            return eventInfoRepo.findByGroupEventId(dto.getGroupEventId())
                .next()
                .flatMap(existing -> {
                    String creatorId = existing.getCreatorId();
                    if (creatorId != null && !creatorId.trim().isEmpty() && !creatorId.equals(userId)) {
                        return Mono.error(new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.FORBIDDEN,
                            "Only the event creator can edit or reschedule this event"
                        ));
                    }
                    return performRescheduleOrCancel(dto, userId);
                })
                .switchIfEmpty(Mono.defer(() -> performRescheduleOrCancel(dto, userId)));
        } else if (dto.getChatId() != null && dto.getMsgId() != null && !dto.getMsgId().trim().isEmpty()) {
            return eventInfoRepo.findByChatIdAndMsgId(dto.getChatId(), dto.getMsgId().trim())
                .next()
                .flatMap(existing -> {
                    String creatorId = existing.getCreatorId();
                    if (creatorId != null && !creatorId.trim().isEmpty() && !creatorId.equals(userId)) {
                        return Mono.error(new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.FORBIDDEN,
                            "Only the event creator can edit or reschedule this event"
                        ));
                    }
                    return performRescheduleOrCancel(dto, userId);
                })
                .switchIfEmpty(Mono.defer(() -> performRescheduleOrCancel(dto, userId)));
        }

        return performRescheduleOrCancel(dto, userId);
    }

    private Mono<String> performRescheduleOrCancel(GroupEventCreateDTO dto, String userId) {
        Instant start = dto.getStartTime();
        Instant end = dto.getEndTime() != null ? dto.getEndTime() : (start != null ? start.plus(1, ChronoUnit.HOURS) : null);

        Mono<Integer> syncMono = Mono.just(0);

        if (dto.getGroupEventId() != null && !dto.getGroupEventId().isEmpty()) {
            syncMono = eventInfoRepo.syncGroupEvent(
                dto.getGroupEventId(),
                dto.getEventTitle(),
                start,
                end,
                dto.getEventDetails(),
                dto.getEventColor(),
                dto.getAction(),
                dto.getMeetingType(),
                dto.getMeetingLink(),
                dto.getLocation()
            );
        } else if (dto.getChatId() != null && dto.getMsgId() != null && !dto.getMsgId().trim().isEmpty()) {
            syncMono = eventInfoRepo.syncByChatAndMsgId(
                dto.getChatId(),
                dto.getMsgId().trim(),
                dto.getEventTitle(),
                start,
                end,
                dto.getEventDetails(),
                dto.getEventColor(),
                dto.getAction(),
                dto.getMeetingType(),
                dto.getMeetingLink(),
                dto.getLocation()
            );
        } else if (dto.getEventId() != null) {
            syncMono = eventInfoRepo.findById(dto.getEventId())
                .flatMap(existing -> {
                    existing.setEventTitle(dto.getEventTitle());
                    existing.setStartTime(start);
                    existing.setEndTime(end);
                    existing.setEventDetails(dto.getEventDetails());
                    existing.setEventColor(dto.getEventColor());
                    existing.setAction(dto.getAction());
                    existing.setMeetingType(dto.getMeetingType());
                    existing.setMeetingLink(dto.getMeetingLink());
                    existing.setLocation(dto.getLocation());
                    return eventInfoRepo.save(existing).thenReturn(1);
                })
                .defaultIfEmpty(0);
        }

        return syncMono.flatMap(rowsUpdated -> {
            if (dto.getChatId() != null && !dto.getChatId().isEmpty()) {
                EventSyncMessageDTO syncMsg = new EventSyncMessageDTO();
                syncMsg.setType("event_sync");
                syncMsg.setGroupEventId(dto.getGroupEventId());
                syncMsg.setChatId(dto.getChatId());
                syncMsg.setMsgId(dto.getMsgId());
                syncMsg.setEventTitle(dto.getEventTitle());
                syncMsg.setStartTime(start);
                syncMsg.setEndTime(end);
                syncMsg.setAction(dto.getAction());
                syncMsg.setEventColor(dto.getEventColor());
                syncMsg.setMeetingType(dto.getMeetingType());
                syncMsg.setMeetingLink(dto.getMeetingLink());
                syncMsg.setLocation(dto.getLocation());
                syncMsg.setUpdatedBy(userId);

                // Send push notification to group members (offline recipients)
                String summary = "Event updated";
                if ("cancelled".equalsIgnoreCase(dto.getAction())) {
                    summary = "Meeting cancelled";
                } else if (start != null) {
                    summary = "Rescheduled to " + start.toString();
                } else if (dto.getLocation() != null && !dto.getLocation().isEmpty()) {
                    summary = "Location updated: " + dto.getLocation();
                }
                final String finalSummary = summary;
                final String startTimeStr = start != null ? start.toString() : "";

                groupInfoRepo.getMembers(dto.getChatId())
                    .map(m -> m.getUserId())
                    .filter(mId -> !mId.equals(userId))
                    .doOnNext(memberId -> {
                        notificationService.sendEventChange(
                            memberId,
                            dto.getGroupEventId(),
                            dto.getEventTitle(),
                            finalSummary,
                            startTimeStr
                        );
                    })
                    .subscribe();

                triggerEventBannerAndNotification(dto.getChatId(), userId, dto.getGroupEventId(), dto.getEventTitle(), dto.getAction(), start);
                return myWebSocketHandler.broadcastEventSync(dto.getChatId(), userId, syncMsg)
                    .thenReturn("updated");
            }
            return Mono.just("updated");
        });
    }

    // ================= RSVP / VOTING =================
    public Mono<Map<String, Long>> updateRsvp(String groupEventId, String userId, String rsvpStatus, String chatId) {
        return eventInfoRepo.updateRsvpStatus(groupEventId, userId, rsvpStatus)
            .then(getRsvpCounts(groupEventId))
            .flatMap(counts -> {
                if (chatId != null && !chatId.isEmpty()) {
                    EventSyncMessageDTO syncMsg = new EventSyncMessageDTO();
                    syncMsg.setType("event_rsvp_update");
                    syncMsg.setGroupEventId(groupEventId);
                    syncMsg.setChatId(chatId);
                    syncMsg.setRsvpCounts(counts);
                    syncMsg.setUpdatedBy(userId);

                    return myWebSocketHandler.broadcastToChat(chatId, syncMsg).thenReturn(counts);
                }
                return Mono.just(counts);
            });
    }

    public Mono<Map<String, Long>> getRsvpCounts(String groupEventId) {
        return eventInfoRepo.countRsvps(groupEventId)
            .collectMap(RsvpCountDTO::getRsvpStatus, RsvpCountDTO::getCount);
    }

    // ================= PERSONAL REMINDER PREFERENCES =================
    public Mono<String> updateReminder(EventReminderDTO dto, String userId) {
        return eventInfoRepo.updateReminderSettings(
            dto.getEventId(),
            userId,
            dto.getReminderEnabled(),
            dto.getReminderOffset(),
            dto.getCustomReminderTime()
        ).thenReturn("ok");
    }

    // ================= CONFLICT CHECKING =================
    public Mono<Boolean> hasConflict(String userId, Instant start, Instant end) {
        return eventInfoRepo.countOverlappingEvents(userId, start, end)
            .map(count -> count > 0);
    }

    // ================= PUBLIC EXTERNAL BOOKING =================
    public Mono<List<PublicBookingSlotDTO>> getPublicBusySlots(String userIdentifier, LocalDate date) {
        return userInfoRepo.findByBookingSlugOrUser(userIdentifier)
            .flatMapMany(user -> {
                if (user.getIsBookingEnabled() != null && !user.getIsBookingEnabled()) {
                    return Flux.empty(); // Booking disabled by host
                }
                if (!user.getShowBusySlotsExternal()) {
                    return Flux.empty(); // privacy mode: zero slots exposed
                }
                Instant startOfDay = date.atStartOfDay().toInstant(ZoneOffset.UTC);
                Instant endOfDay = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

                return eventInfoRepo.findUserEventsForDay(user.getUserId(), startOfDay, endOfDay)
                    .map(ev -> new PublicBookingSlotDTO(ev.getStartTime(), ev.getEndTime()));
            })
            .collectList();
    }

    public Mono<Map<String, Object>> getPublicHostInfo(String userIdentifier) {
        return userInfoRepo.findByBookingSlugOrUser(userIdentifier)
            .switchIfEmpty(Mono.error(new RuntimeException("Host not found")))
            .map(user -> {
                Map<String, Object> info = new HashMap<>();
                info.put("userId", user.getUserId());
                info.put("userName", user.getPublicName() != null ? user.getPublicName() : user.getUserId());
                info.put("profilePic", user.getProfilePic());
                info.put("bookingSlug", user.getBookingSlug() != null ? user.getBookingSlug() : user.getUserId());
                info.put("isBookingEnabled", user.getIsBookingEnabled() != null ? user.getIsBookingEnabled() : true);
                info.put("showBusySlots", user.getShowBusySlotsExternal() != null ? user.getShowBusySlotsExternal() : false);
                return info;
            });
    }

    public Mono<Map<String, Object>> requestPublicBooking(String userIdentifier, PublicBookingRequestDTO dto) {
        return userInfoRepo.findByBookingSlugOrUser(userIdentifier)
            .switchIfEmpty(Mono.error(new RuntimeException("Host not found")))
            .flatMap(host -> {
                if (host.getIsBookingEnabled() != null && !host.getIsBookingEnabled()) {
                    return Mono.error(new RuntimeException("Host is currently not accepting new bookings."));
                }

                Instant start = dto.getStartTime();
                Instant end = dto.getEndTime() != null ? dto.getEndTime() : start.plus(1, ChronoUnit.HOURS);

                return hasConflict(host.getUserId(), start, end)
                    .flatMap(conflict -> {
                        if (conflict) {
                            return Mono.error(new RuntimeException("Selected time slot is no longer available."));
                        }

                        String bookingToken = UUID.randomUUID().toString();

                        EventInfo ev = new EventInfo();
                        ev.setUserId(host.getUserId());
                        ev.setEventTitle(dto.getAgenda() != null ? dto.getAgenda() : "Meeting Request");
                        ev.setStartTime(start);
                        ev.setEndTime(end);
                        ev.setEventDetails("Meeting with " + dto.getBookerName() + " (" + dto.getBookerEmail() + ")");
                        ev.setMeetingType(dto.getMeetingType() != null ? dto.getMeetingType() : "online");
                        ev.setMeetingLink(dto.getMeetingLink());
                        ev.setLocation(dto.getLocation());
                        ev.setExternalBookerName(dto.getBookerName());
                        ev.setExternalBookerEmail(dto.getBookerEmail());
                        ev.setExternalStatus("pending");
                        ev.setIsAdded(false);
                        ev.setIsSync(true);
                        ev.setReminderEnabled(true);
                        ev.setReminderOffset(60);
                        ev.setBookingToken(bookingToken);
                        ev.setGuestUserId(dto.getGuestUserId());
                        ev.setLastModifiedBy("GUEST");
                        ev.setChangeMessage(dto.getAgenda());

                        return eventInfoRepo.save(ev)
                            .flatMap(saved -> {
                                Map<String, Object> alert = new HashMap<>();
                                alert.put("type", "booking_request");
                                alert.put("eventId", saved.getEventId());
                                alert.put("bookingToken", bookingToken);
                                alert.put("agenda", dto.getAgenda());
                                alert.put("bookerName", dto.getBookerName());
                                alert.put("bookerEmail", dto.getBookerEmail());
                                alert.put("guestUserId", dto.getGuestUserId());
                                alert.put("startTime", start.toString());
                                alert.put("endTime", end.toString());
                                alert.put("meetingType", dto.getMeetingType());
                                alert.put("meetingLink", dto.getMeetingLink());
                                alert.put("location", dto.getLocation());

                                myWebSocketHandler.sendToUser(host.getUserId(), alert);

                                // Send push notification to Host (in case offline)
                                notificationService.sendEventChange(
                                    host.getUserId(),
                                    String.valueOf(saved.getEventId()),
                                    dto.getAgenda() != null ? dto.getAgenda() : "New Meeting Request",
                                    (dto.getBookerName() != null ? dto.getBookerName() : "A user") + " booked a meeting",
                                    start.toString()
                                );

                                // If guest is also a LetsChat user: NO email, send WebSocket update
                                if (dto.getGuestUserId() != null && !dto.getGuestUserId().isEmpty()) {
                                    Map<String, Object> guestAlert = new HashMap<>(alert);
                                    guestAlert.put("type", "booking_created");
                                    myWebSocketHandler.sendToUser(dto.getGuestUserId(), guestAlert);

                                    Map<String, Object> result = new HashMap<>();
                                    result.put("status", "success");
                                    result.put("message", "Meeting request submitted successfully.");
                                    result.put("bookingToken", bookingToken);
                                    result.put("eventId", saved.getEventId());
                                    return Mono.just(result);
                                }

                                // External Guest: Send Transactional Email with magic link & LetsChat download banner
                                String hostName = host.getPublicName() != null ? host.getPublicName() : "Host";
                                String manageLink = frontendUrl + "/booking/view/" + bookingToken;
                                String emailHtml = buildGuestEmailHtml(
                                    hostName,
                                    dto.getAgenda(),
                                    start.toString(),
                                    end.toString(),
                                    dto.getMeetingType(),
                                    dto.getLocation() != null ? dto.getLocation() : dto.getMeetingLink(),
                                    manageLink,
                                    "Your meeting request has been submitted to <strong>" + hostName + "</strong>."
                                );

                                return brevoMailService.sendEmail(
                                    dto.getBookerEmail(),
                                    "Meeting Request Submitted: " + dto.getAgenda(),
                                    emailHtml
                                ).map(sent -> {
                                    Map<String, Object> result = new HashMap<>();
                                    result.put("status", "success");
                                    result.put("message", "Meeting request submitted successfully. Confirmation email sent.");
                                    result.put("bookingToken", bookingToken);
                                    result.put("eventId", saved.getEventId());
                                    return result;
                                });
                            });
                    });
            });
    }

    public Mono<Map<String, Object>> getBookingDetails(String tokenOrId, String authUserId) {
        Mono<EventInfo> findMono;
        if (tokenOrId != null && tokenOrId.matches("\\d+")) {
            findMono = eventInfoRepo.findById(Long.parseLong(tokenOrId))
                .switchIfEmpty(eventInfoRepo.findByBookingToken(tokenOrId));
        } else {
            findMono = eventInfoRepo.findByBookingToken(tokenOrId);
        }

        return findMono
            .switchIfEmpty(Mono.error(new RuntimeException("Booking not found")))
            .flatMap(ev -> {
                boolean isHost = authUserId != null && authUserId.equals(ev.getUserId());
                boolean isGuest = (authUserId != null && authUserId.equals(ev.getGuestUserId()))
                    || (ev.getBookingToken() != null && ev.getBookingToken().equalsIgnoreCase(tokenOrId));

                return userInfoRepo.findByIdentifier(ev.getUserId())
                    .defaultIfEmpty(new UserInfo())
                    .flatMap(host -> {
                        Mono<UserInfo> guestMono = (ev.getGuestUserId() != null)
                            ? userInfoRepo.findByIdentifier(ev.getGuestUserId()).defaultIfEmpty(new UserInfo())
                            : Mono.just(new UserInfo());

                        return guestMono.map(guestUser -> {
                            Map<String, Object> res = new HashMap<>();
                            res.put("eventId", ev.getEventId());
                            res.put("bookingToken", ev.getBookingToken());
                            res.put("eventTitle", ev.getEventTitle());
                            res.put("eventDetails", ev.getEventDetails());
                            res.put("startTime", ev.getStartTime());
                            res.put("endTime", ev.getEndTime());
                            res.put("meetingType", ev.getMeetingType());
                            res.put("meetingLink", ev.getMeetingLink());
                            res.put("location", ev.getLocation());
                            res.put("externalStatus", ev.getExternalStatus());
                            res.put("action", ev.getAction());
                            res.put("lastModifiedBy", ev.getLastModifiedBy());
                            res.put("changeMessage", ev.getChangeMessage());
                            res.put("hostUserId", ev.getUserId());
                            res.put("hostName", host.getPublicName() != null ? host.getPublicName() : host.getUserId());
                            res.put("hostProfilePic", host.getProfilePic());
                            res.put("guestUserId", ev.getGuestUserId());
                            res.put("bookerName", ev.getGuestUserId() != null && guestUser.getPublicName() != null ? guestUser.getPublicName() : ev.getExternalBookerName());
                            res.put("bookerEmail", ev.getExternalBookerEmail());
                            res.put("guestProfilePic", guestUser.getProfilePic());
                            res.put("isHost", isHost);
                            res.put("isGuest", isGuest);
                            res.put("canManage", isHost || isGuest);
                            return res;
                        });
                    });
            });
    }

    public Mono<Map<String, Object>> respondToBooking(String tokenOrId, Map<String, Object> body, String authUserId) {
        Mono<EventInfo> findMono;
        if (tokenOrId != null && tokenOrId.matches("\\d+")) {
            findMono = eventInfoRepo.findById(Long.parseLong(tokenOrId))
                .switchIfEmpty(eventInfoRepo.findByBookingToken(tokenOrId));
        } else {
            findMono = eventInfoRepo.findByBookingToken(tokenOrId);
        }

        return findMono
            .switchIfEmpty(Mono.error(new RuntimeException("Booking not found")))
            .flatMap(ev -> {
                boolean isHost = authUserId != null && authUserId.equals(ev.getUserId());
                boolean isGuest = (authUserId != null && authUserId.equals(ev.getGuestUserId()))
                    || (ev.getBookingToken() != null && ev.getBookingToken().equalsIgnoreCase(tokenOrId));

                if (!isHost && !isGuest) {
                    return Mono.error(new RuntimeException("Unauthorized to modify this booking"));
                }

                String responder = isHost ? "HOST" : "GUEST";
                String action = body.get("action") != null ? body.get("action").toString().toUpperCase() : "";
                String message = body.get("message") != null ? body.get("message").toString() : null;

                switch (action) {
                    case "ACCEPT":
                        ev.setExternalStatus("approved");
                        ev.setAction("scheduled");
                        ev.setIsAdded(true);
                        ev.setIsSync(true);
                        ev.setLastModifiedBy(responder);
                        ev.setChangeMessage(message != null ? message : (responder.equals("HOST") ? "Host accepted the meeting." : "Guest accepted the meeting."));
                        break;

                    case "PROPOSE_TIME":
                        if (body.get("startTime") != null) {
                            ev.setStartTime(Instant.parse(body.get("startTime").toString()));
                        }
                        if (body.get("endTime") != null) {
                            ev.setEndTime(Instant.parse(body.get("endTime").toString()));
                        }
                        ev.setExternalStatus(responder.equals("HOST") ? "proposed_by_host" : "proposed_by_guest");
                        ev.setLastModifiedBy(responder);
                        ev.setChangeMessage(message != null ? message : (responder + " proposed a new time."));
                        break;

                    case "CHANGE_LOCATION":
                        if (body.get("meetingType") != null) {
                            ev.setMeetingType(body.get("meetingType").toString());
                        }
                        if (body.get("meetingLink") != null) {
                            ev.setMeetingLink(body.get("meetingLink").toString());
                        }
                        if (body.get("location") != null) {
                            ev.setLocation(body.get("location").toString());
                        }
                        ev.setExternalStatus(responder.equals("HOST") ? "location_changed_by_host" : "location_changed_by_guest");
                        ev.setLastModifiedBy(responder);
                        ev.setChangeMessage(message != null ? message : (responder + " updated the meeting location/link."));
                        break;

                    case "CANCEL":
                    case "DECLINE":
                        ev.setExternalStatus("cancelled");
                        ev.setAction("cancelled");
                        ev.setIsAdded(false);
                        ev.setLastModifiedBy(responder);
                        ev.setChangeMessage(message != null ? message : (responder + " cancelled the meeting."));
                        break;

                    default:
                        return Mono.error(new RuntimeException("Unknown response action: " + action));
                }

                return eventInfoRepo.save(ev)
                    .flatMap(saved -> {
                        Map<String, Object> updateAlert = new HashMap<>();
                        updateAlert.put("type", "booking_update");
                        updateAlert.put("eventId", saved.getEventId());
                        updateAlert.put("bookingToken", saved.getBookingToken());
                        updateAlert.put("externalStatus", saved.getExternalStatus());
                        updateAlert.put("lastModifiedBy", saved.getLastModifiedBy());
                        updateAlert.put("changeMessage", saved.getChangeMessage());
                        updateAlert.put("startTime", saved.getStartTime().toString());
                        updateAlert.put("endTime", saved.getEndTime().toString());
                        updateAlert.put("meetingType", saved.getMeetingType());
                        updateAlert.put("meetingLink", saved.getMeetingLink());
                        updateAlert.put("location", saved.getLocation());

                        // Send WebSocket to Host
                        myWebSocketHandler.sendToUser(saved.getUserId(), updateAlert);

                        // Send push notification to the other party (offline recipient)
                        String recipientUserId = isHost ? saved.getGuestUserId() : saved.getUserId();
                        if (recipientUserId != null && !recipientUserId.isEmpty()) {
                            notificationService.sendEventChange(
                                recipientUserId,
                                String.valueOf(saved.getEventId()),
                                saved.getEventTitle() != null ? saved.getEventTitle() : "Meeting Booking",
                                saved.getChangeMessage() != null ? saved.getChangeMessage() : "Meeting details changed to " + saved.getExternalStatus(),
                                saved.getStartTime() != null ? saved.getStartTime().toString() : ""
                            );
                        }

                        // If guest is LetsChat user: send WebSocket to Guest, NO email!
                        if (saved.getGuestUserId() != null && !saved.getGuestUserId().isEmpty()) {
                            myWebSocketHandler.sendToUser(saved.getGuestUserId(), updateAlert);
                            Map<String, Object> res = new HashMap<>();
                            res.put("status", "success");
                            res.put("externalStatus", saved.getExternalStatus());
                            res.put("message", "Booking updated successfully.");
                            return Mono.just(res);
                        }

                        // If guest is external AND Host updated it -> Send email notification to guest
                        if (saved.getGuestUserId() == null && isHost && saved.getExternalBookerEmail() != null) {
                            return userInfoRepo.findByIdentifier(saved.getUserId())
                                .defaultIfEmpty(new UserInfo())
                                .flatMap(hostUser -> {
                                    String hostName = hostUser.getPublicName() != null ? hostUser.getPublicName() : "Host";
                                    String manageLink = frontendUrl + "/booking/view/" + saved.getBookingToken();
                                    String note = "Meeting update from <strong>" + hostName + "</strong>: " + (saved.getChangeMessage() != null ? saved.getChangeMessage() : saved.getExternalStatus());

                                    String emailHtml = buildGuestEmailHtml(
                                        hostName,
                                        saved.getEventTitle(),
                                        saved.getStartTime().toString(),
                                        saved.getEndTime().toString(),
                                        saved.getMeetingType(),
                                        saved.getLocation() != null ? saved.getLocation() : saved.getMeetingLink(),
                                        manageLink,
                                        note
                                    );

                                    return brevoMailService.sendEmail(
                                        saved.getExternalBookerEmail(),
                                        "Meeting Update: " + saved.getEventTitle() + " (" + saved.getExternalStatus() + ")",
                                        emailHtml
                                    ).map(sent -> {
                                        Map<String, Object> res = new HashMap<>();
                                        res.put("status", "success");
                                        res.put("externalStatus", saved.getExternalStatus());
                                        res.put("message", "Booking updated and email notification sent to guest.");
                                        return res;
                                    });
                                });
                        }

                        Map<String, Object> res = new HashMap<>();
                        res.put("status", "success");
                        res.put("externalStatus", saved.getExternalStatus());
                        res.put("message", "Booking updated successfully.");
                        return Mono.just(res);
                    });
            });
    }

    public Flux<Map<String, Object>> getUserBookings(String userId) {
        return eventInfoRepo.findBookingsForUser(userId)
            .flatMap(ev -> {
                boolean isHost = userId.equals(ev.getUserId());
                String otherUserId = isHost ? ev.getGuestUserId() : ev.getUserId();

                Mono<UserInfo> otherUserMono = (otherUserId != null && !otherUserId.isEmpty())
                    ? userInfoRepo.findByIdentifier(otherUserId).defaultIfEmpty(new UserInfo())
                    : Mono.just(new UserInfo());

                return otherUserMono.map(other -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("eventId", ev.getEventId());
                    map.put("bookingToken", ev.getBookingToken());
                    map.put("eventTitle", ev.getEventTitle());
                    map.put("eventDetails", ev.getEventDetails());
                    map.put("startTime", ev.getStartTime());
                    map.put("endTime", ev.getEndTime());
                    map.put("meetingType", ev.getMeetingType());
                    map.put("meetingLink", ev.getMeetingLink());
                    map.put("location", ev.getLocation());
                    map.put("externalStatus", ev.getExternalStatus());
                    map.put("action", ev.getAction());
                    map.put("lastModifiedBy", ev.getLastModifiedBy());
                    map.put("changeMessage", ev.getChangeMessage());
                    map.put("isHost", isHost);
                    map.put("hostUserId", ev.getUserId());
                    map.put("guestUserId", ev.getGuestUserId());
                    if (isHost) {
                        map.put("otherPartyName", ev.getGuestUserId() != null && other.getPublicName() != null ? other.getPublicName() : ev.getExternalBookerName());
                        map.put("otherPartyEmail", ev.getExternalBookerEmail());
                        map.put("otherPartyPic", other.getProfilePic());
                    } else {
                        map.put("otherPartyName", other.getPublicName() != null ? other.getPublicName() : other.getUserId());
                        map.put("otherPartyEmail", other.getEmail());
                        map.put("otherPartyPic", other.getProfilePic());
                    }
                    return map;
                });
            });
    }

    public Mono<String> handleBookingApproval(Long eventId, String hostUserId, boolean approve) {
        return eventInfoRepo.findById(eventId)
            .switchIfEmpty(Mono.error(new RuntimeException("Event not found")))
            .flatMap(ev -> {
                if (!ev.getUserId().equals(hostUserId)) {
                    return Mono.error(new RuntimeException("Unauthorized"));
                }

                if (approve) {
                    ev.setExternalStatus("approved");
                    ev.setIsAdded(true);
                    ev.setIsSync(true);
                    ev.setReminderEnabled(true);
                    ev.setReminderOffset(60);
                } else {
                    ev.setExternalStatus("declined");
                    ev.setAction("cancelled");
                }

                return eventInfoRepo.save(ev).map(saved -> {
                    if (saved.getGuestUserId() != null && !saved.getGuestUserId().isEmpty()) {
                        notificationService.sendEventChange(
                            saved.getGuestUserId(),
                            String.valueOf(saved.getEventId()),
                            saved.getEventTitle() != null ? saved.getEventTitle() : "Meeting Booking",
                            approve ? "Host approved the meeting request" : "Host declined the meeting request",
                            saved.getStartTime() != null ? saved.getStartTime().toString() : ""
                        );
                    }
                    return approve ? "approved" : "declined";
                });
            });
    }

    public Mono<String> updateBookingPrivacy(String userId, Boolean showBusySlots, Boolean isBookingEnabled) {
        Mono<Integer> s1 = (showBusySlots != null)
            ? userInfoRepo.updateShowBusySlots(userId, showBusySlots)
            : Mono.just(1);
        Mono<Integer> s2 = (isBookingEnabled != null)
            ? userInfoRepo.updateIsBookingEnabled(userId, isBookingEnabled)
            : Mono.just(1);
        return s1.then(s2).thenReturn("ok");
    }

    private String buildGuestEmailHtml(
        String hostName,
        String agenda,
        String startTime,
        String endTime,
        String meetingType,
        String locationOrLink,
        String manageLink,
        String statusNote
    ) {
        return """
            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px; background-color: #ffffff;">
                <div style="text-align: center; margin-bottom: 24px;">
                    <h2 style="color: #0f172a; margin: 0 0 8px 0;">LetsChat Meeting Schedule</h2>
                    <p style="color: #64748b; margin: 0; font-size: 15px;">%s</p>
                </div>
                <div style="background-color: #f8fafc; border-radius: 8px; padding: 20px; margin-bottom: 24px; border-left: 4px solid #6366f1;">
                    <p style="margin: 0 0 10px 0; font-size: 16px;"><strong>Topic:</strong> %s</p>
                    <p style="margin: 0 0 10px 0; font-size: 14px; color: #334155;"><strong>Host:</strong> %s</p>
                    <p style="margin: 0 0 10px 0; font-size: 14px; color: #334155;"><strong>Start:</strong> %s</p>
                    <p style="margin: 0 0 10px 0; font-size: 14px; color: #334155;"><strong>End:</strong> %s</p>
                    <p style="margin: 0; font-size: 14px; color: #334155;"><strong>Location/Link:</strong> %s</p>
                </div>
                <div style="text-align: center; margin-bottom: 32px;">
                    <a href="%s" style="background-color: #6366f1; color: #ffffff; padding: 12px 28px; font-weight: 600; text-decoration: none; border-radius: 8px; display: inline-block;">View, Reschedule or Manage Meeting</a>
                </div>
                <div style="border-top: 1px solid #e2e8f0; padding-top: 20px; text-align: center; background-color: #f1f5f9; border-radius: 8px; padding: 16px;">
                    <p style="margin: 0; color: #475569; font-size: 14px;">
                        Want to chat directly with <strong>%s</strong>? 
                        <a href="%s" style="color: #6366f1; font-weight: 600; text-decoration: none;">Download LetsChat</a> to connect instantly!
                    </p>
                </div>
            </div>
            """.formatted(
                statusNote,
                agenda != null ? agenda : "Discussion",
                hostName,
                startTime,
                endTime,
                locationOrLink != null ? locationOrLink : "To be decided",
                manageLink,
                hostName,
                appDownloadUrl
            );
    }
}

