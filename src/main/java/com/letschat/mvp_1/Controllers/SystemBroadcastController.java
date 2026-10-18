package com.letschat.mvp_1.Controllers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.letschat.mvp_1.Models.SystemBroadcast;
import com.letschat.mvp_1.Repositories.SystemBroadcastRepo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "📢 System Broadcasts", description = "Endpoints for checking system announcements and broadcasts")
@CrossOrigin(originPatterns = "*")
@RestController
@RequestMapping({"/api/system/messages", "/system/messages"})
public class SystemBroadcastController {

    private final SystemBroadcastRepo systemBroadcastRepo;

    public SystemBroadcastController(SystemBroadcastRepo systemBroadcastRepo) {
        this.systemBroadcastRepo = systemBroadcastRepo;
    }

    @GetMapping("/check")
    public Mono<ResponseEntity<Map<String, Object>>> checkSystemBroadcasts(
            @RequestParam(required = false) String lastTimestamp,
            @RequestHeader(value = "User-Id", required = false) String userId) {

        Flux<SystemBroadcast> broadcastFlux;

        if (lastTimestamp != null && !lastTimestamp.isBlank()) {
            LocalDateTime cutoff;
            try {
                cutoff = LocalDateTime.parse(lastTimestamp, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e1) {
                try {
                    cutoff = LocalDateTime.ofInstant(Instant.parse(lastTimestamp), ZoneOffset.UTC);
                } catch (Exception e2) {
                    cutoff = LocalDateTime.now().minusDays(30);
                }
            }
            broadcastFlux = systemBroadcastRepo.findNewerThan(cutoff);
        } else {
            // First time load: return latest 50 announcements in chronological order
            broadcastFlux = systemBroadcastRepo.findLatest(50)
                    .collectList()
                    .flatMapMany(list -> {
                        Collections.reverse(list);
                        return Flux.fromIterable(list);
                    });
        }

        return broadcastFlux.collectList()
                .map(messages -> {
                    if (messages.isEmpty()) {
                        return ResponseEntity.ok(Map.<String, Object>of(
                                "hasNew", false,
                                "messages", Collections.emptyList(),
                                "latestTimestamp", lastTimestamp != null ? lastTimestamp : ""
                        ));
                    }

                    List<Map<String, Object>> formattedMessages = new ArrayList<>();
                    String latestTs = "";

                    for (SystemBroadcast b : messages) {
                        String createdAtIso = b.getCreatedAt() != null ? b.getCreatedAt().toString() : "";
                        latestTs = createdAtIso;

                        Map<String, Object> msgMap = new HashMap<>();
                        msgMap.put("msgId", b.getMsgId());
                        msgMap.put("type", b.getType());
                        msgMap.put("content", b.getContent());
                        msgMap.put("caption", b.getCaption() != null ? b.getCaption() : "");
                        msgMap.put("createdAt", createdAtIso);
                        formattedMessages.add(msgMap);
                    }

                    Map<String, Object> response = new HashMap<>();
                    response.put("hasNew", true);
                    response.put("messages", formattedMessages);
                    response.put("latestTimestamp", latestTs);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(e -> {
                    System.err.println("[SystemBroadcastController] Error checking broadcasts: " + e.getMessage());
                    return Mono.just(ResponseEntity.ok(Map.of(
                            "hasNew", false,
                            "messages", Collections.emptyList(),
                            "latestTimestamp", lastTimestamp != null ? lastTimestamp : ""
                    )));
                });
    }
}
