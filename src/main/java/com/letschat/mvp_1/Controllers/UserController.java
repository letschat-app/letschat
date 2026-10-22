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
    private final com.letschat.mvp_1.Service.UsernameValidationService usernameValidationService;
    private final com.letschat.mvp_1.Repositories.UserChatInfoRepo userChatInfoRepo;
    private final com.letschat.mvp_1.NotificationService notificationService;

    public UserController(UserSignUpService userSignUpService,UserLoginService userLoginService,
        UserSearchService userSearchService,UserChatService userChatService,
        GroupCreateService groupCreateService,ChatBoxService chatBoxService,
        UserProfileService userProfileService,EventService eventService,
        MyWebSocketHandler myWebSocketHandler,AdminService adminService,
        OtpService otpService, UserInfoRepo userInfoRepo,
        com.letschat.mvp_1.Service.UsernameValidationService usernameValidationService,
        com.letschat.mvp_1.Repositories.UserChatInfoRepo userChatInfoRepo,
        com.letschat.mvp_1.NotificationService notificationService){
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
        this.usernameValidationService=usernameValidationService;
        this.userChatInfoRepo=userChatInfoRepo;
        this.notificationService=notificationService;
    }

    @PostMapping("/create")
    public Mono<ResponseEntity<String>> signup(@RequestBody UserSignUpDTO request) {
        System.out.println("creating");
        return userSignUpService.createUser(request)
        .map(message->ResponseEntity.status(201).body(message));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, Object>>> login(@RequestBody UserLoginDTO request){
        return userLoginService.logUser(request)
        .map(result -> {
            String status = (String) result.get("status");
            if ("SUCCESS".equals(status) || "2FA_REQUIRED".equals(status)) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(400).body(result);
            }
        });
    }

    @PostMapping("/login/2fa-verify")
    public Mono<ResponseEntity<Map<String, Object>>> verify2FALogin(@RequestBody Map<String, String> body) {
        String identifier = body.get("identifier");
        if (identifier == null) identifier = body.get("userId");
        String otp = body.get("otp");

        if (identifier == null || otp == null) {
            return Mono.just(ResponseEntity.status(400).body(Map.of("status", "ERROR", "message", "identifier and otp are required")));
        }

        return userLoginService.verify2FALogin(identifier, otp)
                .map(user -> {
                    Map<String, Object> data = new java.util.HashMap<>();
                    data.put("status", "SUCCESS");
                    data.put("message", "2FA verified, login successful");
                    data.put("userId", user.getUserId());
                    data.put("username", user.getPublicName());
                    data.put("displayName", user.getPrivateName() != null && !user.getPrivateName().trim().isEmpty() ? user.getPrivateName() : user.getPublicName());
                    data.put("email", user.getEmail() != null ? user.getEmail() : "");
                    data.put("profile", user.getUserProfilePath() != null ? user.getUserProfilePath() : "");
                    data.put("is2FAEnabled", true);
                    return ResponseEntity.ok(data);
                })
                .defaultIfEmpty(ResponseEntity.status(400).body(Map.of("status", "ERROR", "message", "Invalid or expired 2FA OTP")));
    }

    @PostMapping("/2fa/send-otp")
    public Mono<ResponseEntity<Map<String, String>>> send2FAConfigOtp(
            @RequestHeader(value = "User-Id", required = false) String headerUserId,
            @RequestParam(required = false, defaultValue = "true") boolean enable,
            @RequestBody(required = false) Map<String, Object> body) {
        String userId = headerUserId;
        if (userId == null && body != null && body.get("userId") != null) {
            userId = String.valueOf(body.get("userId"));
        }
        if (body != null && body.containsKey("enable")) {
            enable = Boolean.parseBoolean(String.valueOf(body.get("enable")));
        }
        if (userId == null || userId.trim().isEmpty()) {
            return Mono.just(ResponseEntity.status(400).body(Map.of("status", "error", "message", "User-Id is required")));
        }
        final boolean finalEnable = enable;
        return userInfoRepo.findByIdentifier(userId)
                .flatMap(user -> {
                    String targetEmail = user.getEmail() != null ? user.getEmail() : user.getUserId();
                    return otpService.send2FAConfigOtp(user.getUserId(), targetEmail, finalEnable)
                            .map(sent -> sent
                                ? ResponseEntity.ok(Map.of("status", "success", "message", "Verification code sent to registered email"))
                                : ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to send 2FA verification email")));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found"))));
    }

    @PostMapping("/2fa/verify-and-toggle")
    public Mono<ResponseEntity<Map<String, Object>>> verifyAndToggle2FA(
            @RequestHeader(value = "User-Id", required = false) String headerUserId,
            @RequestBody Map<String, Object> body) {
        String userId = headerUserId;
        if (userId == null && body.get("userId") != null) {
            userId = String.valueOf(body.get("userId"));
        }
        String otp = body.get("otp") != null ? String.valueOf(body.get("otp")) : null;
        boolean enable = body.containsKey("enable") ? Boolean.parseBoolean(String.valueOf(body.get("enable"))) : true;

        if (userId == null || otp == null) {
            return Mono.just(ResponseEntity.status(400).body(Map.of("status", "error", "message", "User-Id and otp are required")));
        }

        final boolean finalEnable = enable;
        return userInfoRepo.findByIdentifier(userId)
                .flatMap(user -> {
                    boolean valid = otpService.verify2FAConfigOtp(user.getUserId(), otp);
                    if (!valid) {
                        return Mono.just(ResponseEntity.status(400).body(Map.<String, Object>of(
                            "status", "error",
                            "message", "Invalid or expired 2FA OTP"
                        )));
                    }
                    return userInfoRepo.update2FAStatus(user.getUserId(), finalEnable)
                            .map(updated -> ResponseEntity.ok(Map.<String, Object>of(
                                "status", "success",
                                "is2FAEnabled", updated.getIs2FAEnabled(),
                                "message", "Two-factor authentication successfully " + (finalEnable ? "enabled" : "disabled")
                            )));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found"))));
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

    @PostMapping("/update-display-name")
    public Mono<ResponseEntity<Object>> updateDisplayName(
        @RequestHeader("User-Id") String userId,
        @RequestBody Map<String, String> body
    ) {
        String displayName = body.get("displayName");
        if (displayName == null || displayName.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().<Object>body(Map.of("status", "ERROR", "message", "Display name cannot be empty")));
        }
        String cleanDisplayName = displayName.trim();

        return userInfoRepo.findByUserId(userId)
            .flatMap(user -> {
                // 1. Self-similarity check (>= 3.0 out of 10 with permanent username)
                double simScore = com.letschat.mvp_1.Service.UsernameSimilarityEvaluator.computeSelfSimilarity(user.getPublicName(), cleanDisplayName);
                if (simScore < 3.0) {
                    return Mono.just(ResponseEntity.badRequest().<Object>body(Map.of(
                        "status", "SIMILARITY_TOO_LOW",
                        "score", simScore,
                        "message", "Display name must have at least 3/10 similarity with your permanent username (@" + user.getPublicName() + "). Current score: " + simScore + "/10"
                    )));
                }

                // 2. Atomic 24-hour cooldown check directly in database query
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.LocalDateTime cooldownThreshold = now.minusHours(24);

                return userInfoRepo.updateDisplayNameAtomic(cleanDisplayName, now, cooldownThreshold, userId)
                    .flatMap(updatedUser -> {
                        // Cascading update to uncustomized chats
                        return userChatInfoRepo.propagateDisplayName(userId, cleanDisplayName)
                            .thenReturn(ResponseEntity.ok().<Object>body(Map.of(
                                "status", "SUCCESS",
                                "displayName", updatedUser.getPrivateName(),
                                "username", updatedUser.getPublicName(),
                                "userId", updatedUser.getUserId(),
                                "lastDisplayNameChangeAt", updatedUser.getLastDisplayNameChangeAt() != null ? updatedUser.getLastDisplayNameChangeAt().toString() : now.toString()
                            )));
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        // If 0 rows updated, check remaining cooldown
                        if (user.getLastDisplayNameChangeAt() != null) {
                            java.time.Duration elapsed = java.time.Duration.between(user.getLastDisplayNameChangeAt(), now);
                            if (elapsed.toHours() < 24) {
                                long remainingMinutes = (24 * 60) - elapsed.toMinutes();
                                long hours = remainingMinutes / 60;
                                long mins = remainingMinutes % 60;
                                return Mono.just(ResponseEntity.status(429).<Object>body(Map.of(
                                    "status", "COOLDOWN_ACTIVE",
                                    "remainingMinutes", remainingMinutes,
                                    "message", "Display name can only be changed once every 24 hours. You can change it again in " + hours + "h " + mins + "m"
                                )));
                            }
                        }
                        return Mono.just(ResponseEntity.badRequest().<Object>body(Map.of("status", "ERROR", "message", "Unable to update display name")));
                    }));
            })
            .defaultIfEmpty(ResponseEntity.status(404).<Object>body(Map.of("status", "ERROR", "message", "User not found")));
    }

    @GetMapping("/check-display-name")
    public Mono<ResponseEntity<Object>> checkDisplayName(
        @RequestHeader("User-Id") String userId,
        @RequestParam("name") String name
    ) {
        if (name == null || name.trim().isEmpty()) {
            return Mono.just(ResponseEntity.ok().<Object>body(Map.of("allowed", false, "score", 0.0, "message", "Name cannot be empty")));
        }
        return userInfoRepo.findByUserId(userId)
            .map(user -> {
                double score = com.letschat.mvp_1.Service.UsernameSimilarityEvaluator.computeSelfSimilarity(user.getPublicName(), name.trim());
                boolean allowed = score >= 3.0;
                boolean cooldownActive = false;
                long remainingMinutes = 0;
                if (user.getLastDisplayNameChangeAt() != null) {
                    java.time.Duration elapsed = java.time.Duration.between(user.getLastDisplayNameChangeAt(), java.time.LocalDateTime.now());
                    if (elapsed.toHours() < 24) {
                        cooldownActive = true;
                        remainingMinutes = (24 * 60) - elapsed.toMinutes();
                    }
                }
                return ResponseEntity.ok().<Object>body(Map.of(
                    "allowed", allowed,
                    "score", score,
                    "cooldownActive", cooldownActive,
                    "remainingMinutes", remainingMinutes,
                    "message", allowed ? "Name is valid" : "Must have at least 3/10 similarity with @" + user.getPublicName()
                ));
            })
            .defaultIfEmpty(ResponseEntity.status(404).build());
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
    public Mono<ResponseEntity<String>> addspace(
            @RequestBody ChatSpace data,
            @RequestHeader(value = "User-Id", required = false) String userId,
            @RequestHeader(value = "User-Name", required = false) String userName) {
        return userChatService.addspace(data, userId, userName)
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
    

    @GetMapping({"/check-username", "/check-name"})
    @Operation(summary = "Check Username Availability & Similarity", description = "Verifies username availability and evaluates similarity against existing usernames across UserInfo and UserChat.")
    public Mono<ResponseEntity<com.letschat.mvp_1.DTOs.UsernameCheckResultDTO>> checkUsername(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String excludeUserId) {
        String target = (username != null && !username.trim().isEmpty()) ? username : name;
        return usernameValidationService.checkUsername(target, excludeUserId)
                .map(ResponseEntity::ok);
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

    @PostMapping("/forgot-password/verify-otp")
    public Mono<ResponseEntity<Map<String, String>>> verifyForgotPasswordOtp(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        String otp = request.get("otp");

        if (identifier == null || otp == null) {
            return Mono.just(ResponseEntity.status(400).body(Map.of("status", "error", "message", "identifier and otp are required")));
        }

        String resetToken = otpService.verifyForgotPasswordOtpAndIssueToken(identifier, otp);
        if (resetToken != null) {
            return Mono.just(ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "resetToken", resetToken,
                "message", "OTP verified successfully. You may now reset your password."
            )));
        } else {
            return Mono.just(ResponseEntity.status(400).body(Map.of(
                "status", "ERROR",
                "message", "Invalid or expired OTP"
            )));
        }
    }

    @PostMapping("/forgot-password/reset")
    public Mono<ResponseEntity<Map<String, String>>> resetPassword(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        String resetToken = request.get("resetToken");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        if (identifier == null || newPassword == null || (resetToken == null && otp == null)) {
            return Mono.just(ResponseEntity.status(400).body(Map.of(
                "status", "error",
                "message", "identifier, newPassword, and resetToken (or otp) are required"
            )));
        }

        boolean valid = false;
        if (resetToken != null && !resetToken.trim().isEmpty()) {
            valid = otpService.verifyAndConsumeResetToken(identifier, resetToken);
        } else if (otp != null && !otp.trim().isEmpty()) {
            valid = otpService.verifyForgotPasswordOtp(identifier, otp);
        }

        if (!valid) {
            return Mono.just(ResponseEntity.status(400).body(Map.of("status", "error", "message", "Invalid or expired reset token/OTP")));
        }

        return userInfoRepo.updatePassword(identifier, newPassword)
                .map(user -> ResponseEntity.ok(Map.of("status", "success", "message", "Password reset successful")))
                .switchIfEmpty(Mono.just(ResponseEntity.status(400).body(Map.of("status", "error", "message", "User not found"))));
    }

    @PostMapping("/change-password/send-otp")
    public Mono<ResponseEntity<Map<String, String>>> sendChangePasswordOtp(
            @RequestHeader(value = "User-Id", required = false) String headerUserId,
            @RequestBody(required = false) Map<String, String> body) {
        String userId = headerUserId;
        if (userId == null && body != null && body.get("userId") != null) {
            userId = body.get("userId");
        }
        if (userId == null || userId.trim().isEmpty()) {
            return Mono.just(ResponseEntity.status(400).body(Map.of("status", "error", "message", "User-Id is required")));
        }

        return userInfoRepo.findByIdentifier(userId)
                .flatMap(user -> {
                    String targetEmail = user.getEmail() != null ? user.getEmail() : user.getUserId();
                    return otpService.sendPasswordChangeOtp(user.getUserId(), targetEmail)
                            .map(sent -> sent
                                ? ResponseEntity.ok(Map.of("status", "success", "message", "Password change OTP sent to your registered email"))
                                : ResponseEntity.status(500).body(Map.of("status", "error", "message", "Failed to send OTP email")));
                })
                .switchIfEmpty(Mono.just(ResponseEntity.status(404).body(Map.of("status", "error", "message", "User not found"))));
    }

    @PostMapping("/change-password")
    public Mono<ResponseEntity<Map<String, Object>>> changePassword(
            @RequestHeader(value = "User-Id", required = false) String headerUserId,
            @RequestBody Map<String, String> request) {
        String userId = headerUserId;
        if (userId == null && request.get("userId") != null) {
            userId = request.get("userId");
        }
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        String otp = request.get("otp");

        if (userId == null || currentPassword == null || newPassword == null) {
            return Mono.just(ResponseEntity.status(400).body(Map.<String, Object>of(
                "status", "ERROR",
                "message", "User-Id (header or body), currentPassword, and newPassword are required"
            )));
        }

        return userInfoRepo.findByIdentifier(userId)
            .flatMap(user -> {
                // 1. Verify current password
                if (!user.getPassword().equals(currentPassword)) {
                    return Mono.just(ResponseEntity.status(400).body(Map.<String, Object>of(
                        "status", "ERROR",
                        "message", "Incorrect current password"
                    )));
                }

                // 2. Check if 2FA is enabled
                if (Boolean.TRUE.equals(user.getIs2FAEnabled())) {
                    // If no OTP provided, send OTP and request 2FA verification
                    if (otp == null || otp.trim().isEmpty()) {
                        String targetEmail = user.getEmail() != null ? user.getEmail() : user.getUserId();
                        return otpService.sendPasswordChangeOtp(user.getUserId(), targetEmail)
                            .map(sent -> ResponseEntity.ok(Map.<String, Object>of(
                                "status", "2FA_REQUIRED",
                                "message", "2FA is enabled. A verification code has been sent to your registered email to confirm password change."
                            )));
                    }

                    // OTP was provided: verify it
                    boolean otpValid = otpService.verifyPasswordChangeOtp(user.getUserId(), otp);
                    if (!otpValid) {
                        return Mono.just(ResponseEntity.status(400).body(Map.<String, Object>of(
                            "status", "ERROR",
                            "message", "Invalid or expired OTP"
                        )));
                    }
                }

                // 3. Update password in database
                return userInfoRepo.updatePassword(user.getUserId(), newPassword)
                    .map(updated -> ResponseEntity.ok(Map.<String, Object>of(
                        "status", "SUCCESS",
                        "message", "Password changed successfully"
                    )));
            })
            .switchIfEmpty(Mono.just(ResponseEntity.status(404).body(Map.<String, Object>of(
                "status", "ERROR",
                "message", "User not found"
            ))));
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

    @Operation(summary = "Get Message Sender Info", description = "Retrieves the chat-specific nickname and sender ID for a given message ID.")
    @GetMapping("/message/sender-info")
    public Mono<ResponseEntity<com.letschat.mvp_1.DTOs.SenderInfoDTO>> getMessageSenderInfo(@RequestParam String msgId) {
        return userChatInfoRepo.findSenderInChat(msgId)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.ok(new com.letschat.mvp_1.DTOs.SenderInfoDTO(null, "Member")));
    }

    @Operation(summary = "Toggle Chat Mute", description = "Globally mutes or unmutes a chat for the authenticated user.")
    @PostMapping("/chat/mute")
    public Mono<ResponseEntity<Map<String, Object>>> toggleMute(
            @RequestParam String chatId,
            @RequestParam boolean isMuted,
            @RequestHeader("User-Id") String userId) {
        return userChatInfoRepo.updateMuteStatus(chatId, userId, isMuted)
            .doOnSuccess(v -> notificationService.updateMuteCache(userId, chatId, isMuted))
            .map(updatedCount -> ResponseEntity.ok(Map.<String, Object>of(
                "status", "success",
                "chatId", chatId,
                "isMuted", isMuted
            )));
    }
}
