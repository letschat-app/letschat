package com.letschat.mvp_1.Controllers;

import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.letschat.mvp_1.Models.UserTokenModel;
import com.letschat.mvp_1.Service.UserTokenService;

import reactor.core.publisher.Mono;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "🔔 FCM Push Notifications", description = "Endpoints for registering FCM device push tokens")
@CrossOrigin(originPatterns="*")
@RestController
@RequestMapping("/api/fcm")
public class FCMController {

   private UserTokenService userTokenService;
   public FCMController(UserTokenService service){
    this.userTokenService=service;
   }


    @PostMapping("/register")
    public Mono<UserTokenModel> register(@RequestBody Map<String,String> body){

        String userId = body.get("userId");
        String deviceId = body.get("deviceId");
        String token = body.get("token");

        return userTokenService.registerToken(userId, deviceId, token);
    }


    @PostMapping("/remove")
    public Mono<Void> remove(@RequestBody Map<String,String> body){

        String deviceId = body.get("deviceId");

        return userTokenService.removeToken(deviceId);

    }

}