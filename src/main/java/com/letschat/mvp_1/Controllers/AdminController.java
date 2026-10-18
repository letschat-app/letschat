package com.letschat.mvp_1.Controllers;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.letschat.mvp_1.DTOs.PulseCreateRequest;
import com.letschat.mvp_1.Models.PulseInfo;
import com.letschat.mvp_1.Models.SystemBroadcast;
import com.letschat.mvp_1.Repositories.SystemBroadcastRepo;
import com.letschat.mvp_1.Service.AdminService;
import com.letschat.mvp_1.Service.PulseService;
import com.letschat.mvp_1.Service.LabsService;
import org.springframework.web.bind.annotation.PathVariable;

import reactor.core.publisher.Mono;

@CrossOrigin(originPatterns="*")
@RestController
@RequestMapping("api/admin")
public class AdminController {

    private final AdminService adminService;
    private final SystemBroadcastRepo systemBroadcastRepo;
    private final PulseService pulseService;
    private final LabsService labsService;

    public AdminController(AdminService adminService, SystemBroadcastRepo systemBroadcastRepo, PulseService pulseService, LabsService labsService) {
        this.adminService = adminService;
        this.systemBroadcastRepo = systemBroadcastRepo;
        this.pulseService = pulseService;
        this.labsService = labsService;
    }

    @GetMapping("/signals")
    public ResponseEntity<?> getSignals(@RequestParam String pass) {

        if (!"ily".equals(pass)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        return ResponseEntity.ok(adminService.getAllSignals());
    }

    @GetMapping("/retention")
    public Map<String, Object> getRetention(
        @RequestParam String fromDate,
        @RequestParam String toDate) {

        double retention = adminService.getRetention(fromDate, toDate);

        return Map.of(
            "fromDate", fromDate,
            "toDate", toDate,
            "retentionPercent", retention
        );
    }

    @GetMapping("/space-adoption")
    public Mono<Map<String, Object>> getSpaceStats() {
        return adminService.getSpaceAdoptionStats();
    }

    @PostMapping("/broadcast/message")
    public Mono<ResponseEntity<Object>> createBroadcastMessage(
            @RequestBody Map<String, String> body,
            @RequestParam(required = false, defaultValue = "ily") String pass) {

        if (!"ily".equals(pass)) {
            return Mono.just(ResponseEntity.status(401).body("Unauthorized"));
        }

        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body("content cannot be empty"));
        }

        String type = body.getOrDefault("type", "text");
        String caption = body.get("caption");
        String msgId = "SYS_" + System.currentTimeMillis();

        SystemBroadcast broadcast = new SystemBroadcast(msgId, type, content, caption, LocalDateTime.now());
        return systemBroadcastRepo.save(broadcast)
                .map(saved -> ResponseEntity.status(201).body((Object) saved))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body((Object) ("Failed to save broadcast: " + e.getMessage()))));
    }

    @PostMapping("/broadcast/pulse")
    public Mono<ResponseEntity<Object>> createBroadcastPulse(
            @RequestBody PulseCreateRequest request,
            @RequestParam(required = false, defaultValue = "ily") String pass) {

        if (!"ily".equals(pass)) {
            return Mono.just(ResponseEntity.status(401).body("Unauthorized"));
        }

        request.setVisibility("everyone");
        return pulseService.createPulse(request, "SYSTEM_ADMIN")
                .map(pulse -> ResponseEntity.status(201).body((Object) pulse))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body((Object) ("Failed to create system pulse: " + e.getMessage()))));
    }

    @GetMapping("/dashboard-metrics")
    public Mono<ResponseEntity<Object>> getDashboardMetrics(
            @RequestParam(required = false, defaultValue = "ily") String pass) {
        if (!"ily".equals(pass)) {
            return Mono.just(ResponseEntity.status(401).body("Unauthorized"));
        }
        return adminService.getDashboardMetrics()
                .map(metrics -> ResponseEntity.ok((Object) metrics))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body((Object) ("Failed to fetch dashboard metrics: " + e.getMessage()))));
    }

    @PostMapping("/labs/experiments")
    public Mono<ResponseEntity<Object>> createExperiment(
            @RequestBody Map<String, String> body,
            @RequestParam(required = false, defaultValue = "ily") String pass) {
        if (!"ily".equals(pass)) {
            return Mono.just(ResponseEntity.status(401).body("Unauthorized"));
        }
        String title = body.get("title");
        String description = body.get("description");
        String status = body.getOrDefault("status", "active");

        return labsService.createExperiment(title, description, status)
                .map(exp -> ResponseEntity.status(201).body((Object) exp))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body((Object) e.getMessage())));
    }

    @GetMapping("/labs/experiments/{id}/metrics")
    public Mono<ResponseEntity<Object>> getExperimentMetrics(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "ily") String pass) {
        if (!"ily".equals(pass)) {
            return Mono.just(ResponseEntity.status(401).body("Unauthorized"));
        }
        return labsService.getExperimentMetrics(id)
                .map(metrics -> ResponseEntity.ok((Object) metrics))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body((Object) e.getMessage())));
    }
}
