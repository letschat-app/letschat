package com.letschat.mvp_1.Controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.letschat.mvp_1.DTOs.MyPulsesResponseDTO;
import com.letschat.mvp_1.DTOs.PulseCreateRequest;
import com.letschat.mvp_1.DTOs.PulseFeedResponseDTO;
import com.letschat.mvp_1.DTOs.PulseSaveMediaRequest;
import com.letschat.mvp_1.DTOs.PulseUploadUrlRequest;
import com.letschat.mvp_1.DTOs.PulseUploadUrlResponse;
import com.letschat.mvp_1.Models.PulseInfo;
import com.letschat.mvp_1.Service.PulseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@Tag(name = "⚡ Pulse System", description = "Endpoints for 24-hour Stories/Pulses, Presigned R2 Storage URLs, Feed, and View Tracking")
@CrossOrigin(originPatterns = "*")
@RestController
@RequestMapping("/api/pulse")
public class PulseController {

    private final PulseService pulseService;

    public PulseController(PulseService pulseService) {
        this.pulseService = pulseService;
    }

    @Operation(summary = "Generate Presigned R2 Storage Upload URLs", description = "Requests presigned S3/R2 put URLs for uploading photo, audio, or video pulse media.")
    @PostMapping("/put-urls")
    public Mono<PulseUploadUrlResponse> getUploadUrls(@RequestBody PulseUploadUrlRequest request) {
        return pulseService.generateUploadUrls(request);
    }

    @Operation(summary = "Register Uploaded Media Keys", description = "Saves uploaded file keys and returns a unified pulseMediaId.")
    @PostMapping("/save-media")
    public Mono<Map<String, Long>> saveMedia(@RequestBody PulseSaveMediaRequest request) {
        return pulseService.saveMedia(request)
                .map(id -> Map.of("pulseMediaId", id));
    }

    @Operation(summary = "Publish a Pulse (Text, Image, Audio, Image+Audio, Video)", description = "Publishes a new 24-hour pulse status update and increments user daily pulse streak.")
    @PostMapping("/create")
    public Mono<ResponseEntity<Object>> createPulse(
            @RequestHeader("User-Id") String userId,
            @RequestBody PulseCreateRequest request) {
        System.out.println("=== [POST /api/pulse/create] Received Request ===");
        System.out.println("User-Id Header: " + userId);
        System.out.println("Payload: type=" + (request != null ? request.getType() : "null")
                + ", content=" + (request != null ? request.getContent() : "null")
                + ", pulseMediaId=" + (request != null ? request.getPulseMediaId() : "null")
                + ", bgColor=" + (request != null ? request.getBgColor() : "null")
                + ", fontStyle=" + (request != null ? request.getFontStyle() : "null"));

        return pulseService.createPulse(request, userId)
                .map(pulse -> {
                    System.out.println("[PulseController] Successfully created pulse ID: " + pulse.getPulseId());
                    return ResponseEntity.status(201).<Object>body(pulse);
                })
                .onErrorResume(e -> {
                    System.err.println("=== [PulseController] ERROR CREATING PULSE ===");
                    System.err.println("Exception: " + e.getClass().getName() + " - " + e.getMessage());
                    e.printStackTrace();
                    String causeMsg = e.getCause() != null ? e.getCause().getMessage() : "None";
                    Map<String, Object> errBody = Map.of(
                            "status", 500,
                            "error", e.getMessage() != null ? e.getMessage() : "Internal Server Error",
                            "cause", causeMsg,
                            "exceptionType", e.getClass().getName()
                    );
                    return Mono.just(ResponseEntity.status(500).body(errBody));
                });
    }

    @Operation(summary = "Get My Active & Expired Pulses", description = "Retrieves current user's active/expired pulses along with total view counts and viewer user list.")
    @GetMapping("/mine")
    public Mono<MyPulsesResponseDTO> getMyPulses(@RequestHeader("User-Id") String userId) {
        return pulseService.getMyPulses(userId);
    }

    @Operation(summary = "Get Contacts Pulse Feed", description = "Retrieves active pulses posted by contacts, grouped by user with streak & view state.")
    @GetMapping("/feed")
    public Mono<PulseFeedResponseDTO> getContactsFeed(@RequestHeader("User-Id") String userId) {
        return pulseService.getContactsFeed(userId);
    }

    @Operation(summary = "Record Pulse View", description = "Marks a specific pulse as viewed by the current user.")
    @PostMapping("/view/{pulseId}")
    public Mono<ResponseEntity<String>> recordView(
            @PathVariable Long pulseId,
            @RequestHeader("User-Id") String viewerUserId) {
        return pulseService.recordView(pulseId, viewerUserId)
                .thenReturn(ResponseEntity.ok("viewed"));
    }
}
