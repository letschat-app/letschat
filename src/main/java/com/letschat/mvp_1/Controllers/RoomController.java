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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.letschat.mvp_1.DTOs.ChatBoxReturnDTO;
import com.letschat.mvp_1.Models.RoomInfo;
import com.letschat.mvp_1.Service.RoomService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@CrossOrigin(originPatterns="*")
@RestController
@RequestMapping("/api/room")
public class RoomController {
    private final RoomService roomService;
    public RoomController(RoomService roomService){
        this.roomService=roomService;
    }

    @PostMapping("/create")
    public Mono<RoomInfo> createRoom(@RequestBody Map<String,String> body){
        return roomService.createRoom(
            body.get("userId"),
            body.get("roomName"),
            body.get("motto")
        );
    }

    @PostMapping("/join")
    public Mono<String> joinRoom(@RequestParam String roomId,@RequestHeader("User-Id") String userId){
        return roomService.joinroom(roomId, userId);
    }

    @GetMapping("/view/{roomId}")
    public Mono<RoomInfo> viewRoom(@PathVariable String roomId){
        return roomService.view(roomId);
    }

    @GetMapping("/load")
    public Flux<ChatBoxReturnDTO> loadRooms(){
        return roomService.load();
    }

    @GetMapping("/load/{userId}")
    public Flux<ChatBoxReturnDTO> loadUserRooms(@PathVariable String userId){
        return roomService.load(userId);
    }

    @PostMapping("/set/name")
    public Mono<ResponseEntity<String>> putnickname(@RequestBody Map<String,String> body,@RequestHeader("User-id") String userid) {
        
        return roomService.createRoomNickname(body.get("name"),userid)
        .map(msg->ResponseEntity.ok(msg))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed")));
    }

    @GetMapping("/get/name")
    public Mono<ResponseEntity<String>> getnickname(@RequestHeader("User-id") String userid) {
        
        return roomService.getRoomNickName(userid)
        .map(msg->ResponseEntity.ok(msg))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed")));
    }
}
