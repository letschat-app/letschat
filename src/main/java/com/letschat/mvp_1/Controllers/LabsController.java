package com.letschat.mvp_1.Controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.letschat.mvp_1.DTOs.LabsExperimentDTO;
import com.letschat.mvp_1.Service.LabsService;

import reactor.core.publisher.Mono;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "🧪 LetsChat Labs", description = "Endpoints for feature experiments, intent voting, and early access opt-in")
@CrossOrigin(originPatterns = "*")
@RestController
@RequestMapping("/api/labs")
public class LabsController {

    private final LabsService labsService;

    public LabsController(LabsService labsService) {
        this.labsService = labsService;
    }

    @Operation(summary = "List Active Experiments", description = "Returns active concept experiments with user vote and opt-in state")
    @GetMapping("/experiments")
    public Mono<ResponseEntity<List<LabsExperimentDTO>>> listExperiments(
            @RequestHeader(value = "User-Id", required = false) String userId,
            @RequestHeader(value = "user-id", required = false) String lowerUserId) {

        String effectiveUserId = (userId != null && !userId.isBlank()) ? userId : lowerUserId;
        return labsService.listExperiments(effectiveUserId)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(List.of())));
    }

    @Operation(summary = "Vote Intent", description = "Vote on experiment intent: 'use', 'build', or 'both'")
    @PostMapping("/experiments/{id}/vote")
    public Mono<ResponseEntity<Object>> voteIntent(
            @PathVariable Long id,
            @RequestParam String intent,
            @RequestHeader(value = "User-Id", required = false) String userId,
            @RequestHeader(value = "user-id", required = false) String lowerUserId) {

        String effectiveUserId = (userId != null && !userId.isBlank()) ? userId : lowerUserId;
        if (effectiveUserId == null || effectiveUserId.isBlank()) {
            return Mono.just(ResponseEntity.status(401).body((Object) "Unauthorized: Missing User-Id header"));
        }

        return labsService.voteIntent(id, effectiveUserId, intent)
                .map(res -> ResponseEntity.ok((Object) res))
                .onErrorResume(IllegalArgumentException.class, e -> Mono.just(ResponseEntity.badRequest().body((Object) e.getMessage())))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body((Object) e.getMessage())));
    }

    @Operation(summary = "Opt-In Early Access", description = "Sign up for early access testing for an experiment")
    @PostMapping("/experiments/{id}/opt-in")
    public Mono<ResponseEntity<Object>> optInEarlyAccess(
            @PathVariable Long id,
            @RequestHeader(value = "User-Id", required = false) String userId,
            @RequestHeader(value = "user-id", required = false) String lowerUserId) {

        String effectiveUserId = (userId != null && !userId.isBlank()) ? userId : lowerUserId;
        if (effectiveUserId == null || effectiveUserId.isBlank()) {
            return Mono.just(ResponseEntity.status(401).body((Object) "Unauthorized: Missing User-Id header"));
        }

        return labsService.optInEarlyAccess(id, effectiveUserId)
                .map(res -> ResponseEntity.ok((Object) res))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body((Object) e.getMessage())));
    }
}
