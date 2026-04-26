package com.letschat.mvp_1.Controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.letschat.mvp_1.Service.AdminService;

import reactor.core.publisher.Mono;

@CrossOrigin(originPatterns="*")
@RestController
@RequestMapping("api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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

}
