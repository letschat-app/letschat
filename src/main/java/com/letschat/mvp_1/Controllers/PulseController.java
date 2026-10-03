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

import reactor.core.publisher.Mono;

@CrossOrigin(originPatterns = "*")
@RestController
@RequestMapping("/api/pulse")
public class PulseController {

    private final PulseService pulseService;

    public PulseController(PulseService pulseService) {
        this.pulseService = pulseService;
    }

    @PostMapping("/put-urls")
    public Mono<PulseUploadUrlResponse> getUploadUrls(@RequestBody PulseUploadUrlRequest request) {
        return pulseService.generateUploadUrls(request);
    }

    @PostMapping("/save-media")
    public Mono<Map<String, Long>> saveMedia(@RequestBody PulseSaveMediaRequest request) {
        return pulseService.saveMedia(request)
                .map(id -> Map.of("pulseMediaId", id));
    }

    @PostMapping("/create")
    public Mono<ResponseEntity<PulseInfo>> createPulse(
            @RequestHeader("User-Id") String userId,
            @RequestBody PulseCreateRequest request) {
        return pulseService.createPulse(request, userId)
                .map(pulse -> ResponseEntity.status(201).body(pulse));
    }

    @GetMapping("/mine")
    public Mono<MyPulsesResponseDTO> getMyPulses(@RequestHeader("User-Id") String userId) {
        return pulseService.getMyPulses(userId);
    }

    @GetMapping("/feed")
    public Mono<PulseFeedResponseDTO> getContactsFeed(@RequestHeader("User-Id") String userId) {
        return pulseService.getContactsFeed(userId);
    }

    @PostMapping("/view/{pulseId}")
    public Mono<ResponseEntity<String>> recordView(
            @PathVariable Long pulseId,
            @RequestHeader("User-Id") String viewerUserId) {
        return pulseService.recordView(pulseId, viewerUserId)
                .thenReturn(ResponseEntity.ok("viewed"));
    }
}
