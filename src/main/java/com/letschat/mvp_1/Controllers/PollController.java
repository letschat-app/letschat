package com.letschat.mvp_1.Controllers;

import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.letschat.mvp_1.DTOs.CreatePollRequestDTO;
import com.letschat.mvp_1.Service.PollService;
import reactor.core.publisher.Mono;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@Tag(name = "📊 Native Polls Engine", description = "Endpoints for decoupled poll creation, voting, and adding custom options")
@RestController
@RequestMapping("/api/polls")
@CrossOrigin(originPatterns = "*")
public class PollController {

    private final PollService pollService;

    public PollController(PollService pollService) {
        this.pollService = pollService;
    }

    @Operation(summary = "Create Native Poll", description = "Step 1 of decoupled poll creation. Returns a pollId to link with WebSocket message.")
    @PostMapping
    public Mono<ResponseEntity<Object>> createPoll(
            @RequestBody CreatePollRequestDTO dto,
            @RequestHeader(value = "User-Id", required = false) String headerUserId,
            @RequestHeader(value = "user-id", required = false) String lowerHeaderUserId,
            Principal principal) {
        String effectiveHeader = (headerUserId != null && !headerUserId.isBlank()) 
                ? headerUserId 
                : (lowerHeaderUserId != null && !lowerHeaderUserId.isBlank() ? lowerHeaderUserId : null);
        String userId = (effectiveHeader != null) 
                ? effectiveHeader 
                : ((principal != null && principal.getName() != null && !"anonymousUser".equals(principal.getName())) ? principal.getName() : null);

        if (userId == null || userId.isBlank()) {
            return Mono.just(ResponseEntity.status(401).body((Object) "Unauthorized: Missing User-Id header"));
        }

        return pollService.createPoll(userId, dto)
            .map(response -> ResponseEntity.ok((Object) response))
            .onErrorResume(IllegalArgumentException.class, ex -> Mono.just(ResponseEntity.badRequest().body((Object) ex.getMessage())))
            .onErrorResume(ex -> Mono.just(ResponseEntity.status(500).body((Object) ex.getMessage())));
    }

    @Operation(summary = "Get Poll Details by Message ID")
    @GetMapping("/by-msg/{msgId}")
    public Mono<ResponseEntity<Object>> getPollByMsgId(
            @PathVariable String msgId,
            @RequestHeader(value = "User-Id", required = false) String userId) {
        return pollService.buildPollUpdateDTO(null, msgId, userId)
            .map(dto -> ResponseEntity.ok((Object) dto))
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get Poll Details by Poll ID")
    @GetMapping("/{pollId}")
    public Mono<ResponseEntity<Object>> getPollById(
            @PathVariable Long pollId,
            @RequestHeader(value = "User-Id", required = false) String userId) {
        return pollService.buildPollUpdateDTO(pollId, null, userId)
            .map(dto -> ResponseEntity.ok((Object) dto))
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
