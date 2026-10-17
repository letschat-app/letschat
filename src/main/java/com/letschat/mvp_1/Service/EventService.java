package com.letschat.mvp_1.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.letschat.mvp_1.DTOs.*;
import com.letschat.mvp_1.Models.EventInfo;
import com.letschat.mvp_1.Models.UserInfo;
import com.letschat.mvp_1.Repositories.EventInfoRepo;
import com.letschat.mvp_1.Repositories.GroupInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;
import com.letschat.mvp_1.WebSocket.MyWebSocketHandler;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class EventService {
    private final EventInfoRepo eventInfoRepo;
    private final GroupInfoRepo groupInfoRepo;
    private final UserInfoRepo userInfoRepo;
    private final MyWebSocketHandler myWebSocketHandler;

    public EventService(
        EventInfoRepo eventInfoRepo,
        GroupInfoRepo groupInfoRepo,
        UserInfoRepo userInfoRepo,
        MyWebSocketHandler myWebSocketHandler
    ) {
        this.eventInfoRepo = eventInfoRepo;
        this.groupInfoRepo = groupInfoRepo;
        this.userInfoRepo = userInfoRepo;
        this.myWebSocketHandler = myWebSocketHandler;
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
        return eventInfoRepo.save(event).thenReturn("ok");
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
                        ev.setIsAdded(true);
                        ev.setIsSync(true);
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
                        });
                });
        }

        // Scope "PERSONAL" or private calendar event
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
                if (gid != null && !gid.isEmpty()) {
                    return eventInfoRepo.findByGroupEventIdAndUserId(gid, userId)
                        .defaultIfEmpty(groupEvent);
                }
                return Mono.just(groupEvent);
            });
    }

    // ================= RESCHEDULE & CANCEL GROUP SYNC =================
    public Mono<String> rescheduleOrCancel(GroupEventCreateDTO dto, String userId) {
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
        } else if (dto.getChatId() != null && dto.getMsgId() != null) {
            syncMono = eventInfoRepo.syncByChatAndMsgId(
                dto.getChatId(),
                dto.getMsgId(),
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

    public Mono<String> requestPublicBooking(String userIdentifier, PublicBookingRequestDTO dto) {
        return userInfoRepo.findByBookingSlugOrUser(userIdentifier)
            .switchIfEmpty(Mono.error(new RuntimeException("Host not found")))
            .flatMap(host -> {
                Instant start = dto.getStartTime();
                Instant end = dto.getEndTime() != null ? dto.getEndTime() : start.plus(1, ChronoUnit.HOURS);

                return hasConflict(host.getUserId(), start, end)
                    .flatMap(conflict -> {
                        if (conflict) {
                            return Mono.error(new RuntimeException("Selected time slot is no longer available."));
                        }

                        EventInfo ev = new EventInfo();
                        ev.setUserId(host.getUserId());
                        ev.setEventTitle(dto.getAgenda() != null ? dto.getAgenda() : "Meeting Request");
                        ev.setStartTime(start);
                        ev.setEndTime(end);
                        ev.setEventDetails("External request from " + dto.getBookerName() + " (" + dto.getBookerEmail() + ")");
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

                        return eventInfoRepo.save(ev)
                            .doOnSuccess(saved -> {
                                Map<String, Object> alert = new HashMap<>();
                                alert.put("type", "booking_request");
                                alert.put("eventId", saved.getEventId());
                                alert.put("agenda", dto.getAgenda());
                                alert.put("bookerName", dto.getBookerName());
                                alert.put("bookerEmail", dto.getBookerEmail());
                                alert.put("startTime", start.toString());
                                alert.put("endTime", end.toString());
                                alert.put("meetingType", dto.getMeetingType());
                                alert.put("meetingLink", dto.getMeetingLink());
                                alert.put("location", dto.getLocation());

                                myWebSocketHandler.sendToUser(host.getUserId(), alert);
                            })
                            .thenReturn("Meeting request submitted successfully.");
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

                return eventInfoRepo.save(ev).thenReturn(approve ? "approved" : "declined");
            });
    }

    public Mono<String> updateBookingPrivacy(String userId, Boolean showBusySlots) {
        return userInfoRepo.updateShowBusySlots(userId, showBusySlots)
            .thenReturn("ok");
    }
}
