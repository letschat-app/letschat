package com.letschat.mvp_1.Controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.letschat.mvp_1.DTOs.ChatBoxReturnDTO;
import com.letschat.mvp_1.DTOs.LoadMessageDTO;
import com.letschat.mvp_1.Models.AssignmentInfo;
import com.letschat.mvp_1.Models.SubmissionInfo;
import com.letschat.mvp_1.Service.AssignmentService;
import com.letschat.mvp_1.Service.ClassRoomService;
import com.letschat.mvp_1.Service.GroupAddService;
import com.letschat.mvp_1.Service.SubmissionService;

import reactor.core.publisher.Mono;


import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.letschat.mvp_1.DTOs.AssignmentStudentStatusDTO;
import com.letschat.mvp_1.DTOs.ClassroomStudentStatsDTO;
import com.letschat.mvp_1.Service.ClassRoomReportService;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "🏫 Classroom & Assignments", description = "Endpoints for Classrooms, Assignments, Submissions, and Matrix Reports (PDF/Excel/CSV)")
@CrossOrigin(originPatterns="*")
@RestController
@RequestMapping("api/classroom")
public class ClassRoomController {

    private final ClassRoomService classRoomService;
    private final GroupAddService groupAddService;
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final ClassRoomReportService classRoomReportService;

    public ClassRoomController(ClassRoomService classRoomService,
                               GroupAddService groupAddService,
                               AssignmentService assignmentService,
                               SubmissionService submissionService,
                               ClassRoomReportService classRoomReportService){
        this.classRoomService=classRoomService;
        this.groupAddService=groupAddService;
        this.assignmentService=assignmentService;
        this.submissionService=submissionService;
        this.classRoomReportService=classRoomReportService;
    }

    @PostMapping("/create/{name}")
    public Mono<ResponseEntity<String>> create(@PathVariable String name,@RequestHeader("User-Id") String UserId){
        return classRoomService.create(UserId, name)
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed"))); 
    }

    @PostMapping("/update/{id}")
    public Mono<ResponseEntity<String>> update(@PathVariable String id,@RequestBody ChatBoxReturnDTO info){
        return classRoomService.update(id, info)
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("error")));
    }

    @PostMapping("/join/{roomid}")
    public Mono<ResponseEntity<String>> join(@PathVariable String roomid,@RequestHeader("User-Id") String UserId){
        System.out.println("called to join");
        return groupAddService.joinclass(roomid, UserId)
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed"))); 
    }

    @PostMapping("/assignment/post")
    public Mono<ResponseEntity<String>> post(@RequestBody AssignmentInfo assignment){
        return assignmentService.post(assignment)
        .map(id->ResponseEntity.ok(String.valueOf(id)))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed"))); 
    }

    @GetMapping("assignment/get/{id}")
    public Mono<ResponseEntity<AssignmentInfo>> get(@PathVariable Long id){
        return assignmentService.get(id)
        .map(assignment->ResponseEntity.ok(assignment));
    }

    @PostMapping("assignment/submit/{id}")
    public Mono<ResponseEntity<SubmissionInfo>> submit(@PathVariable Long id, @RequestBody SubmissionInfo info){
        if (info.getAssignmentId() == 0 || info.getAssignmentId() == 0L) {
            info.setAssignmentId(id);
        }
        if (info.getTimestamp() == null) {
            info.setTimestamp(java.time.Instant.now());
        }
        return submissionService.insert(info)
        .map(body->ResponseEntity.ok(body));
    }

    @GetMapping("submissions/getall/{id}")
    public Mono<List<SubmissionInfo>> getAll(@PathVariable Long id){
        return submissionService.getallsubmission(id).collectList();
    }

    @GetMapping("submission/get/{id}")
    public Mono<SubmissionInfo> getMethodName(@PathVariable Long id,@RequestHeader("User-Id") String userid) {
        return submissionService.getsubmission(id,userid);
    }

    @GetMapping("count")
    public Mono<Integer> getcount(
            @RequestHeader String chatid,
            @RequestHeader @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestamp){
        return classRoomService.getcount(chatid,timestamp);
    }

    @GetMapping("getlast")
    public Mono<LoadMessageDTO> getlast(
            @RequestHeader String chatid,
            @RequestHeader @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestamp){
        return classRoomService.getlast(chatid,timestamp);
    }

    @GetMapping("/assignment/{assignmentId}/students")
    public Mono<List<AssignmentStudentStatusDTO>> getAssignmentStudentStatuses(@PathVariable Long assignmentId) {
        return classRoomReportService.getAssignmentStudentStatuses(assignmentId);
    }

    @GetMapping("/{roomId}/student-stats")
    public Mono<List<ClassroomStudentStatsDTO>> getClassroomStudentStats(@PathVariable String roomId) {
        return classRoomReportService.getClassroomStudentStats(roomId);
    }

    @GetMapping("/{roomId}/export-report")
    public Mono<ResponseEntity<byte[]>> exportReport(
            @PathVariable String roomId,
            @RequestParam(value = "format", defaultValue = "pdf") String format) {
        return classRoomReportService.generateReport(roomId, format)
                .map(bytes -> {
                    String fmt = format.toLowerCase();
                    MediaType mediaType = MediaType.APPLICATION_PDF;
                    String ext = "pdf";

                    if ("excel".equals(fmt) || "xlsx".equals(fmt)) {
                        mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                        ext = "xlsx";
                    } else if ("csv".equals(fmt)) {
                        mediaType = MediaType.parseMediaType("text/csv");
                        ext = "csv";
                    }

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Classroom_Report_" + roomId + "." + ext + "\"")
                            .contentType(mediaType)
                            .body(bytes);
                });
    }

    @PostMapping("/{roomId}/email-report")
    public Mono<ResponseEntity<Map<String, Object>>> emailReport(
            @PathVariable String roomId,
            @RequestParam String email,
            @RequestParam(value = "format", defaultValue = "pdf") String format) {
        return classRoomReportService.sendReportByEmail(roomId, format, email)
                .map(sent -> {
                    if (sent) {
                        return ResponseEntity.ok(Map.of("status", "success", "message", "Report emailed to " + email));
                    } else {
                        return ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to send email report. Ensure mail server settings are configured."));
                    }
                });
    }
}
