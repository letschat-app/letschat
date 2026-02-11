package com.letschat.mvp_1.Controllers;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.letschat.mvp_1.DTOs.LoadMessageDTO;
import com.letschat.mvp_1.Models.AssignmentInfo;
import com.letschat.mvp_1.Service.AssignmentService;
import com.letschat.mvp_1.Service.ClassRoomService;
import com.letschat.mvp_1.Service.GroupAddService;

import reactor.core.publisher.Mono;

@CrossOrigin(originPatterns="*")
@RestController
@RequestMapping("api/classroom")
public class ClassRoomController {

    private final ClassRoomService classRoomService;
    private final GroupAddService groupAddService;
    private final AssignmentService assignmentService;
    public ClassRoomController(ClassRoomService classRoomService,GroupAddService groupAddService,AssignmentService assignmentService){
        this.classRoomService=classRoomService;
        this.groupAddService=groupAddService;
        this.assignmentService=assignmentService;
    }

    @PostMapping("/create/{name}")
    public Mono<ResponseEntity<String>> create(@PathVariable String name,@RequestHeader("User-Id") String UserId){
        return classRoomService.create(UserId, name)
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed"))); 
    }

    @PostMapping("/join/{roomid}")
    public Mono<ResponseEntity<String>> join(@PathVariable String roomid,@RequestHeader("User-Id") String UserId){
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
    public Mono<ResponseEntity<AssignmentInfo>> post(@PathVariable Long id){
        return assignmentService.get(id)
        .map(assignment->ResponseEntity.ok(assignment));
    }

    @GetMapping("count")
    public Mono<Integer> getcount(@RequestHeader String chatid,@RequestHeader LocalDateTime timestamp){
        return classRoomService.getcount(chatid,timestamp);
    }
    @GetMapping("getlast")
    public Mono<LoadMessageDTO> getlast(@RequestHeader String chatid,@RequestHeader LocalDateTime timestamp){
        return classRoomService.getlast(chatid,timestamp);
    }
}
