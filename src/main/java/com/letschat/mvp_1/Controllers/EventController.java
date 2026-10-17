package com.letschat.mvp_1.Controllers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.letschat.mvp_1.DTOs.*;
import com.letschat.mvp_1.Models.EventInfo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;
import com.letschat.mvp_1.Service.EventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@Tag(name = "📅 Events & Scheduling", description = "Endpoints for group event ecosystem, multi-user sync, RSVP voting, conflict detection, and public Calendly-style booking")
@CrossOrigin(originPatterns = "*")
@RestController
@RequestMapping("/api")
public class EventController {

    private final EventService eventService;
    private final UserInfoRepo userInfoRepo;

    public EventController(EventService eventService, UserInfoRepo userInfoRepo) {
        this.eventService = eventService;
        this.userInfoRepo = userInfoRepo;
    }

    // ================= GROUP EVENT CREATION =================
    @PostMapping("/user/event/create")
    @Operation(summary = "Create Group or Personal Event", description = "Creates an event. For scope ALL in a group, automatically batch-provisions for all members with sync and reminders ON.")
    public Mono<ResponseEntity<EventInfo>> createEvent(
            @RequestHeader("User-Id") String userId,
            @RequestBody GroupEventCreateDTO dto) {
        return eventService.createGroupEvent(dto, userId)
                .map(ResponseEntity::ok);
    }

    // ================= RESCHEDULE OR CANCEL =================
    @PostMapping("/user/event/update")
    @Operation(summary = "Reschedule or Cancel Event", description = "Updates group event across all synced members' calendars.")
    public Mono<ResponseEntity<Map<String, String>>> updateEvent(
            @RequestHeader("User-Id") String userId,
            @RequestBody GroupEventCreateDTO dto) {
        return eventService.rescheduleOrCancel(dto, userId)
                .map(status -> ResponseEntity.ok(Map.of("status", status)));
    }

    // ================= GROUP EVENTS RETRIEVAL =================
    @GetMapping("/user/event/group/{chatId}")
    @Operation(summary = "Get Group Calendar Events", description = "Fetches all events created within this group for members.")
    public Mono<List<EventInfo>> getGroupEvents(
            @PathVariable String chatId,
            @RequestHeader("User-Id") String userId) {
        return eventService.getGroupEvents(chatId, userId).collectList();
    }

    // ================= RSVP / VOTING =================
    @PostMapping("/user/event/rsvp")
    @Operation(summary = "Submit Event RSVP Vote", description = "Vote going, maybe, or cant_join with real-time tally broadcast.")
    public Mono<ResponseEntity<Map<String, Long>>> submitRsvp(
            @RequestHeader("User-Id") String userId,
            @RequestBody EventRsvpDTO dto) {
        return eventService.updateRsvp(dto.getGroupEventId(), userId, dto.getRsvpStatus(), dto.getChatId())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/user/event/rsvp/{groupEventId}")
    @Operation(summary = "Get RSVP Counts", description = "Fetch live attendance tally for a group event.")
    public Mono<ResponseEntity<Map<String, Long>>> getRsvpCounts(@PathVariable String groupEventId) {
        return eventService.getRsvpCounts(groupEventId)
                .map(ResponseEntity::ok);
    }

    // ================= PERSONAL REMINDER PREFERENCES =================
    @PostMapping("/user/event/reminder")
    @Operation(summary = "Update Personal Reminder Preferences", description = "Toggle reminder or configure custom 15-min offset.")
    public Mono<ResponseEntity<Map<String, String>>> updateReminder(
            @RequestHeader("User-Id") String userId,
            @RequestBody EventReminderDTO dto) {
        return eventService.updateReminder(dto, userId)
                .map(status -> ResponseEntity.ok(Map.of("status", status)));
    }

    // ================= CONFLICT CHECKING =================
    @GetMapping("/user/event/check-conflict")
    @Operation(summary = "Check Schedule Collision", description = "Checks if given time window overlaps with user's existing events.")
    public Mono<ResponseEntity<Map<String, Object>>> checkConflict(
            @RequestHeader(value = "User-Id", required = false) String headerUserId,
            @RequestParam(required = false) String userId,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        String effectiveUserId = headerUserId != null ? headerUserId : userId;
        if (effectiveUserId == null) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "User ID required")));
        }
        Instant start = Instant.parse(startTime);
        Instant end = Instant.parse(endTime);
        return eventService.hasConflict(effectiveUserId, start, end)
                .map(hasConflict -> ResponseEntity.ok(Map.of("hasConflict", hasConflict)));
    }

    // ================= PUBLIC EXTERNAL BOOKING =================
    @GetMapping("/public/book/slots/{identifier}")
    @Operation(summary = "Get Public Available/Busy Slots", description = "Returns busy slots for a host if host enabled external busy slots.")
    public Mono<ResponseEntity<List<PublicBookingSlotDTO>>> getPublicSlots(
            @PathVariable String identifier,
            @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        return eventService.getPublicBusySlots(identifier, localDate)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/public/book/request/{identifier}")
    @Operation(summary = "Request Public Meeting", description = "Allows external guest to schedule a meeting with host.")
    public Mono<ResponseEntity<Map<String, String>>> requestBooking(
            @PathVariable String identifier,
            @RequestBody PublicBookingRequestDTO dto) {
        return eventService.requestPublicBooking(identifier, dto)
                .map(msg -> ResponseEntity.ok(Map.of("status", "success", "message", msg)))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()))));
    }

    @PostMapping("/user/event/book/action")
    @Operation(summary = "Host Meeting Request Action", description = "Approve or decline an external meeting booking request.")
    public Mono<ResponseEntity<Map<String, String>>> handleBookingAction(
            @RequestHeader("User-Id") String hostUserId,
            @RequestBody Map<String, Object> body) {
        Long eventId = Long.valueOf(body.get("eventId").toString());
        boolean approve = Boolean.parseBoolean(body.get("approve").toString());
        return eventService.handleBookingApproval(eventId, hostUserId, approve)
                .map(res -> ResponseEntity.ok(Map.of("status", res)))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()))));
    }

    // ================= BOOKING PRIVACY SETTINGS =================
    @PostMapping("/user/event/booking-settings")
    @Operation(summary = "Update External Booking Privacy Settings", description = "Toggle whether external booking link reveals busy blocks.")
    public Mono<ResponseEntity<Map<String, Object>>> updateBookingSettings(
            @RequestHeader("User-Id") String userId,
            @RequestBody Map<String, Object> body) {
        Boolean showBusySlots = Boolean.parseBoolean(body.get("showBusySlots").toString());
        return eventService.updateBookingPrivacy(userId, showBusySlots)
                .map(res -> ResponseEntity.ok(Map.of("status", "success", "showBusySlots", showBusySlots)));
    }

    @GetMapping("/user/event/booking-settings")
    @Operation(summary = "Get External Booking Settings", description = "Fetch host's external booking slug and privacy preferences.")
    public Mono<ResponseEntity<Map<String, Object>>> getBookingSettings(@RequestHeader("User-Id") String userId) {
        return userInfoRepo.findByIdentifier(userId)
                .map(user -> ResponseEntity.ok(Map.<String, Object>of(
                        "showBusySlots", user.getShowBusySlotsExternal() != null ? user.getShowBusySlotsExternal() : false,
                        "bookingSlug", user.getBookingSlug() != null ? user.getBookingSlug() : user.getUserId(),
                        "userId", user.getUserId(),
                        "userName", user.getPublicName() != null ? user.getPublicName() : user.getUserId()
                )))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
