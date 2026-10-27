package com.letschat.mvp_1.Repositories;

import java.time.Instant;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.DTOs.RsvpCountDTO;
import com.letschat.mvp_1.Models.EventInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventInfoRepo extends ReactiveCrudRepository<EventInfo, Long> {

    Flux<EventInfo> findByUserId(String userId);

    Flux<EventInfo> findByGroupEventId(String groupEventId);

    Mono<EventInfo> findByGroupEventIdAndUserId(String groupEventId, String userId);

    @Query("""
        UPDATE "EventInfo" 
        SET action = :action, event_color = :color 
        WHERE event_id = :id
    """)
    Mono<EventInfo> updateaction(Long id, String action, String color);

    @Query("""
        SELECT *
        FROM "EventInfo"
        WHERE start_time BETWEEN :start AND :end
          AND is_sync = true
    """)
    Flux<EventInfo> findEventsBetween(Instant start, Instant end);

    @Query("""
        SELECT *
        FROM "EventInfo"
        WHERE start_time BETWEEN :startOfDay AND :endOfDay
          AND is_sync = true
    """)
    Flux<EventInfo> findTodayEvents(Instant startOfDay, Instant endOfDay);

    // Group Events Discovery Query: retrieves all events tied to a specific chat/group
    @Query("""
        SELECT DISTINCT ON (COALESCE(group_event_id, CAST(event_id AS VARCHAR))) *
        FROM "EventInfo"
        WHERE chat_id = :chatId 
          AND (is_group_visible = true OR is_group_visible IS NULL)
        ORDER BY COALESCE(group_event_id, CAST(event_id AS VARCHAR)), event_id ASC
    """)
    Flux<EventInfo> findGroupEvents(String chatId);

    // Sync Group Event across all members with is_sync = true
    @Modifying
    @Query("""
        UPDATE "EventInfo"
        SET event_title = :title,
            start_time = :startTime,
            end_time = :endTime,
            event_details = :details,
            event_color = :color,
            action = :action,
            meeting_type = :meetingType,
            meeting_link = :meetingLink,
            location = :location
        WHERE group_event_id = :groupEventId 
          AND is_sync = true
    """)
    Mono<Integer> syncGroupEvent(
        String groupEventId,
        String title,
        Instant startTime,
        Instant endTime,
        String details,
        String color,
        String action,
        String meetingType,
        String meetingLink,
        String location
    );

    // Sync by Chat and MsgId if groupEventId is not set
    @Modifying
    @Query("""
        UPDATE "EventInfo"
        SET event_title = :title,
            start_time = :startTime,
            end_time = :endTime,
            event_details = :details,
            event_color = :color,
            action = :action,
            meeting_type = :meetingType,
            meeting_link = :meetingLink,
            location = :location
        WHERE chat_id = :chatId 
          AND msg_id = :msgId 
          AND is_sync = true
    """)
    Mono<Integer> syncByChatAndMsgId(
        String chatId,
        String msgId,
        String title,
        Instant startTime,
        Instant endTime,
        String details,
        String color,
        String action,
        String meetingType,
        String meetingLink,
        String location
    );

    Flux<EventInfo> findByChatIdAndMsgId(String chatId, String msgId);

    // RSVP Update for a user
    @Modifying
    @Query("""
        UPDATE "EventInfo"
        SET rsvp_status = :rsvpStatus
        WHERE (group_event_id = :groupEventId OR CAST(event_id AS VARCHAR) = :groupEventId)
          AND user_id = :userId
    """)
    Mono<Integer> updateRsvpStatus(String groupEventId, String userId, String rsvpStatus);

    // RSVP Count aggregation
    @Query("""
        SELECT rsvp_status AS "rsvpStatus", COUNT(*) AS "count"
        FROM "EventInfo"
        WHERE group_event_id = :groupEventId 
          AND rsvp_status IS NOT NULL
        GROUP BY rsvp_status
    """)
    Flux<RsvpCountDTO> countRsvps(String groupEventId);

    // Personal Reminder Settings update
    @Modifying
    @Query("""
        UPDATE "EventInfo"
        SET reminder_enabled = :enabled,
            reminder_offset = :offset,
            custom_reminder_time = :customTime
        WHERE event_id = :eventId 
          AND user_id = :userId
    """)
    Mono<Integer> updateReminderSettings(Long eventId, String userId, Boolean enabled, Integer offset, String customTime);

    // Find User Busy Slots for public booking availability
    @Query("""
        SELECT *
        FROM "EventInfo"
        WHERE user_id = :userId
          AND (action IS NULL OR action != 'cancelled')
          AND start_time BETWEEN :startOfDay AND :endOfDay
        ORDER BY start_time ASC
    """)
    Flux<EventInfo> findUserEventsForDay(String userId, Instant startOfDay, Instant endOfDay);

    // Conflict detection query: returns count of overlapping events for a user
    @Query("""
        SELECT COUNT(*)
        FROM "EventInfo"
        WHERE user_id = :userId
          AND (action IS NULL OR action != 'cancelled')
          AND start_time < :end
          AND end_time > :start
    """)
    Mono<Long> countOverlappingEvents(String userId, Instant start, Instant end);

    // Scheduler query for dynamic reminders (events within 25 hours where reminder is enabled)
    @Query("""
        SELECT *
        FROM "EventInfo"
        WHERE is_sync = true
          AND (reminder_enabled = true OR reminder_enabled IS NULL)
          AND (action IS NULL OR action != 'cancelled')
          AND start_time BETWEEN :now AND :maxFuture
    """)
    Flux<EventInfo> findEventsForReminderEvaluation(Instant now, Instant maxFuture);

    @Query("""
        SELECT *
        FROM "EventInfo"
        WHERE booking_token = :token
        LIMIT 1
    """)
    Mono<EventInfo> findByBookingToken(String token);

    @Query("""
        SELECT *
        FROM "EventInfo"
        WHERE (user_id = :userId OR guest_user_id = :userId)
          AND external_status IS NOT NULL
        ORDER BY start_time DESC
    """)
    Flux<EventInfo> findBookingsForUser(String userId);
}
