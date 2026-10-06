package com.letschat.mvp_1.Controllers;

import java.util.List;
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

import com.letschat.mvp_1.DTOs.ChatBoxReturnDTO;
import com.letschat.mvp_1.DTOs.UserLoginDTO;
import com.letschat.mvp_1.DTOs.UserSearchResult;
import com.letschat.mvp_1.DTOs.UserSignUpDTO;
import com.letschat.mvp_1.Models.ChatSpace;
import com.letschat.mvp_1.Models.EventInfo;
import com.letschat.mvp_1.Service.AdminService;
import com.letschat.mvp_1.Service.ChatBoxService;
import com.letschat.mvp_1.Service.EventService;
import com.letschat.mvp_1.Service.GroupCreateService;
import com.letschat.mvp_1.Service.UserChatService;
import com.letschat.mvp_1.Service.UserLoginService;
import com.letschat.mvp_1.Service.UserProfileService;
import com.letschat.mvp_1.Service.UserSearchService;
import com.letschat.mvp_1.Service.UserSignUpService;
import com.letschat.mvp_1.WebSocket.MyWebSocketHandler;

import reactor.core.publisher.Mono;

import com.letschat.mvp_1.Service.OtpService;
import com.letschat.mvp_1.Repositories.UserInfoRepo;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "👤 User & Authentication", description = "Endpoints for Sign Up, Login, 2FA Verification, User Search, Explore, and Profile Management")
@CrossOrigin(originPatterns="*")
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    private final UserSignUpService userSignUpService;
    private final UserLoginService userLoginService;
    private final UserSearchService userSearchService;
    private final UserChatService userChatService;
    private final ChatBoxService chatBoxService;
    private final UserProfileService userProfileService;
    private final EventService eventService;
    public final MyWebSocketHandler myWebSocketHandler;
    private final AdminService adminService;
    private final OtpService otpService;
    private final UserInfoRepo userInfoRepo;

    public UserController(UserSignUpService userSignUpService,UserLoginService userLoginService,
        UserSearchService userSearchService,UserChatService userChatService,
        GroupCreateService groupCreateService,ChatBoxService chatBoxService,
        UserProfileService userProfileService,EventService eventService,
        MyWebSocketHandler myWebSocketHandler,AdminService adminService,
        OtpService otpService, UserInfoRepo userInfoRepo){
        this.userSignUpService=userSignUpService;
        this.userLoginService=userLoginService;
        this.userSearchService=userSearchService;
        this.userChatService=userChatService;
        this.chatBoxService=chatBoxService;
        this.userProfileService = userProfileService;
        this.eventService=eventService;
        this.myWebSocketHandler=myWebSocketHandler;
        this.adminService=adminService;
        this.otpService=otpService;
        this.userInfoRepo=userInfoRepo;
    }

    @PostMapping("/create")
    public Mono<ResponseEntity<String>> signup(@RequestBody UserSignUpDTO request) {
        System.out.println("creating");
        return userSignUpService.createUser(request)
        .map(message->ResponseEntity.status(201).body(message));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<Object>> login(@RequestBody UserLoginDTO request){
        return userLoginService.logUser(request)
        .map(result -> {
            if (result.startsWith("2FA_REQUIRED:")) {
                String userId = result.substring("2FA_REQUIRED:".length());
                return ResponseEntity.ok((Object) Map.of(
                    "status", "2FA_REQUIRED",
                    "userId", userId,
                    "message", "2-Factor Authentication OTP sent to your registered email"
                ));
            } else if ("login succesfull".equals(result)) {
                return ResponseEntity.ok((Object) Map.of(
                    "status", "SUCCESS",
                    "message", "login succesfull"
                ));
            } else {
                return ResponseEntity.status(400).body((Object) Map.of(
                    "status", "ERROR",
                    "message", result
                ));
            }
        });
    }

    @PostMapping("/login/2fa-verify")
    public Mono<ResponseEntity<Map<String, String>>> verify2FALogin(@RequestBody Map<String, String> body) {
        String identifier = body.get("identifier");
        if (identifier == null) identifier = body.get("userId");
        String otp = body.get("otp");

        if (identifier == null || otp == null) {
            return Mono.just(ResponseEntity.status(400).body(Map.of("status", "error", "message", "identifier and otp are required")));
        }

        return userLoginService.verify2FALogin(identifier, otp)
                .map(valid -> valid 
                    ? ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "2FA verified, login successful"))
                    : ResponseEntity.status(400).body(Map.of("status", "ERROR", "message", "Invalid or expired 2FA OTP")));
    }

    @PostMapping("/2fa/toggle")
    public Mono<ResponseEntity<Map<String, Object>>> toggle2FA(@RequestHeader("User-Id") String userId, @RequestParam boolean enable) {
        return userInfoRepo.update2FAStatus(userId, enable)
                .map(user -> ResponseEntity.ok(Map.<String, Object>of("status", "success", "is2FAEnabled", user.getIs2FAEnabled())))
                .switchIfEmpty(Mono.just(ResponseEntity.status(400).body(Map.<String, Object>of("status", "error", "message", "User not found"))));
    }

    @GetMapping("/me")
    public Mono<UserSearchResult> getme(@RequestHeader("User-Id") String UserId){
        return userSearchService.getme(UserId);
    }

    @GetMapping("/explore")
    public Mono<List<UserSearchResult>> explore(){
        System.out.println("exloring");
        return userSearchService.explore().collectList();
    }

    @GetMapping("/search/{UserId}")
    public Mono<ResponseEntity<UserSearchResult>> search(@PathVariable String UserId){
        return userSearchService.search(UserId)
        .map(user->ResponseEntity.ok(user))
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @PostMapping("/addtochat/{AddUserId}")
    public Mono<ResponseEntity<String>> addchat(@PathVariable String AddUserId,@RequestHeader("User-Id") String UserId){
        return userChatService.addchat(UserId, AddUserId)
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("error")));
    }

    @GetMapping("/chatbox/{UserId}")
    public Mono<List<ChatBoxReturnDTO>> getchatbox(@PathVariable String UserId){
        return chatBoxService.getchat(UserId).collectList();
       // .doOnNext(data->System.out.println(data.getcAt()));
    }

    //@PostMapping(value="/profile/{UserId}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PostMapping("/profile/{mediaId}")
    public Mono<ResponseEntity<String>> setprofile(@RequestHeader("User-id") String UserId,@PathVariable String mediaId){
        System.out.println("profile"+UserId);
        return userProfileService.upload(mediaId, UserId)
        .map(message->ResponseEntity.ok(message))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed")));
    }

    @PostMapping("/setevent/{UserId}")
    public Mono<ResponseEntity<Long>> setevent(@RequestBody EventInfo event,@PathVariable String UserId) {      
        return eventService.addevent(event, UserId)
        .map(id->ResponseEntity.ok(id));
    }

    @GetMapping("getevents/{UserId}")
    public Mono<List<EventInfo>> getevent(@PathVariable String UserId){
        return eventService.getevents(UserId).collectList();
    }

    @PostMapping("/update")
    public Mono<ResponseEntity<String>> updateaction(@RequestBody EventInfo event){
        System.out.println("updated");
        return eventService.action(event).thenReturn(ResponseEntity.ok("ok"));
    }

    @GetMapping("/getchatinfo/{Chatid}")
    public Mono<UserSearchResult> getInfo(@PathVariable String Chatid,@RequestHeader("User-Id") String Userid){
        return userChatService.getInfo(Chatid,Userid);
    }
    
    @PostMapping("/updatemyinfo/{ChatId}")
    public Mono<ResponseEntity<String>> updateInfo(@PathVariable String ChatId,@RequestBody UserSearchResult info){
        return userChatService.updateInfo(info,ChatId)
        .map(msg->ResponseEntity.ok("ok"))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed")));
    }

    @PostMapping("/add-space")
    public Mono<ResponseEntity<String>> addspace(@RequestBody ChatSpace data){
        return userChatService.addspace(data)
        .map(msg->ResponseEntity.ok("ok"))
        .switchIfEmpty(Mono.just(ResponseEntity.status(400).body("failed")));
    }

    @GetMapping("/get-space/{ChatId}")
    public Mono<List<ChatSpace>> getspace(@PathVariable String ChatId){
        return userChatService.getspace(ChatId).collectList();
    }

    @PostMapping("/check-status")
    public Mono<Map<String,Boolean>> check(@RequestBody List<String> userids){
        System.out.println("status check "+ userids.size());
        return myWebSocketHandler.checkUserStatus(userids);
    }
    

    @GetMapping("/wake-up")
    public Mono<String> wakeup(){
        return Mono.just("alive");
    }

    @PostMapping("/send-signup-otp")
    public Mono<ResponseEntity<Map<String, String>>> sendSignupOtp(@RequestParam String email) {
        return otpService.sendRegistrationOtp(email)
                .map(sent -> sent 
                    ? ResponseEntity.ok(Map.of("status", "success", "message", "OTP sent to " + email))
                    : ResponseEntity.status(400).body(Map.of("status", "error", "message", "Failed to send OTP")));
    }

    @PostMapping("/forgot-password/send-otp")
    public Mono<ResponseEntity<Map<String, String>>> sendForgotPasswordOtp(@RequestParam String identifier) {
        return userInfoRepo.findByIdentifier(identifier)
                .flatMap(user -> {
                    String targetEmail = user.getEmail() != null ? user.getEmail() : identifier;
                    return otpService.sendForgotPasswordOtp(identifier, targetEmail)
                            .map(sent -> sent
                                ? ResponseEntity.ok(Map.of("status", "success", "message", "Reset OTP sent to registered email"))
                                : ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to send OTP email")));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found"))));
    }

    @PostMapping("/forgot-password/reset")
    public Mono<ResponseEntity<Map<String, String>>> resetPassword(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        if (identifier == null || otp == null || newPassword == null) {
            return Mono.just(ResponseEntity.status(400).body(Map.of("status", "error", "message", "identifier, otp, and newPassword are required")));
        }

        boolean valid = otpService.verifyForgotPasswordOtp(identifier, otp);
        if (!valid) {
            return Mono.just(ResponseEntity.status(400).body(Map.of("status", "error", "message", "Invalid or expired OTP")));
        }

        return userInfoRepo.updatePassword(identifier, newPassword)
                .map(user -> ResponseEntity.ok(Map.of("status", "success", "message", "Password reset successful")))
                .switchIfEmpty(Mono.just(ResponseEntity.status(400).body(Map.of("status", "error", "message", "User not found"))));
    }

    @PostMapping("/signup/init")
    public Mono<ResponseEntity<Map<String, String>>> initSignup(@RequestBody UserSignUpDTO request) {
        return userSignUpService.initSignup(request)
                .map(sent -> sent
                    ? ResponseEntity.ok(Map.of("status", "success", "message", "OTP sent to " + request.getEmail()))
                    : ResponseEntity.status(400).body(Map.of("status", "error", "message", "Failed to send OTP or invalid email")));
    }

    @PostMapping("/signup/verify")
    public Mono<ResponseEntity<Object>> verifySignup(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");

        if (email == null || otp == null) {
            return Mono.just(ResponseEntity.status(400).body(Map.of("status", "error", "message", "email and otp are required")));
        }

        return userSignUpService.verifyAndCreateUser(email, otp)
                .map(userResult -> ResponseEntity.status(201).body((Object) userResult))
                .switchIfEmpty(Mono.just(ResponseEntity.status(400).body((Object) Map.of("status", "error", "message", "INVALID_OTP_OR_EXPIRED"))));
    }
}
