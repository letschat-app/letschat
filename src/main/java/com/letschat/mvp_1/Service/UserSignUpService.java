package com.letschat.mvp_1.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.letschat.mvp_1.DTOs.UserSearchResult;
import com.letschat.mvp_1.DTOs.UserSignUpDTO;
import com.letschat.mvp_1.Models.LocationInfo;
import com.letschat.mvp_1.Repositories.IdTableRepo;
import com.letschat.mvp_1.Repositories.LocationInfoRepo;
import com.letschat.mvp_1.Repositories.UserInfoRepo;

import reactor.core.publisher.Mono;

@Component
public class UserSignUpService {
    private final UserInfoRepo userInfoRepo;
    private final LocationInfoRepo locationInfoRepo;
    private final IdTableRepo idTableRepo;
    private final AdminService adminService;
    private final OtpService otpService;
    private final UsernameValidationService usernameValidationService;

    private static final long PENDING_SIGNUP_TTL_SECONDS = 600; // 10 minutes
    private final Map<String, PendingSignupEntry> pendingSignupMap = new ConcurrentHashMap<>();

    private record PendingSignupEntry(UserSignUpDTO dto, Instant expiresAt) {}

    public UserSignUpService(UserInfoRepo userInfoRepo, LocationInfoRepo locationInfoRepo, IdTableRepo idTableRepo, AdminService adminService, OtpService otpService, UsernameValidationService usernameValidationService){
        this.userInfoRepo = userInfoRepo;
        this.locationInfoRepo = locationInfoRepo;
        this.idTableRepo = idTableRepo;
        this.adminService = adminService;
        this.otpService = otpService;
        this.usernameValidationService = usernameValidationService;
    }   

    public Mono<Boolean> initSignup(UserSignUpDTO request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty() || request.getPublicName() == null) {
            return Mono.just(false);
        }
        String cleanEmail = request.getEmail().trim().toLowerCase();

        return usernameValidationService.checkUsername(request.getPublicName(), null)
            .flatMap(checkResult -> {
                if ("BLOCK".equals(checkResult.getDecision())) {
                    System.out.println("[SignUp] Rejected duplicate/similar username: " + request.getPublicName());
                    return Mono.just(false);
                }
                pendingSignupMap.put(cleanEmail, new PendingSignupEntry(request, Instant.now().plusSeconds(PENDING_SIGNUP_TTL_SECONDS)));
                return otpService.sendRegistrationOtp(cleanEmail);
            });
    }

    public Mono<UserSearchResult> verifyAndCreateUser(String email, String otp) {
        if (email == null || otp == null) {
            return Mono.empty();
        }
        String cleanEmail = email.trim().toLowerCase();
        boolean valid = otpService.verifyRegistrationOtp(cleanEmail, otp);
        if (!valid) {
            return Mono.empty();
        }

        PendingSignupEntry entry = pendingSignupMap.remove(cleanEmail);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            return Mono.empty();
        }

        return createUserFromDTO(entry.dto());
    }

    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void cleanExpiredPendingSignups() {
        Instant now = Instant.now();
        pendingSignupMap.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }

    private Mono<UserSearchResult> persistAndWelcomeUser(String id, UserSignUpDTO request, long locationId) {
        return userInfoRepo.insertWithEmail(id, request.getPrivateName(), request.getPublicName(), request.getAge(), request.getGender(), locationId, request.getPassword(), request.getEmail())
            .doOnSuccess(user -> {
                adminService.onUserRegistered(id);
                String targetEmail = (user != null && user.getEmail() != null) ? user.getEmail() : request.getEmail();
                String username = (user != null && user.getPublicName() != null) ? user.getPublicName() : request.getPublicName();
                if (targetEmail != null && !targetEmail.trim().isEmpty()) {
                    otpService.sendWelcomeEmail(targetEmail, username, id)
                        .subscribe(
                            sent -> System.out.println("[SignUp] Welcome email dispatched to " + targetEmail + ": " + sent),
                            err -> System.err.println("[SignUp] Failed to dispatch welcome email: " + err.getMessage())
                        );
                }
            })
            .map(user -> {
                UserSearchResult result = new UserSearchResult();
                result.setUserId(id);
                result.setUserName(user.getPublicName());
                result.setProfile(user.getUserProfilePath());
                return result;
            });
    }

    public Mono<UserSearchResult> createUserFromDTO(UserSignUpDTO request) {
        return locationInfoRepo.findExact(request.getStateName(), request.getDistrictName(), request.getVillageName())
            .flatMap(location -> generateId().flatMap(id -> persistAndWelcomeUser(id, request, location.getLocationId())))
            .switchIfEmpty(Mono.defer(() -> {
                System.out.println("switch:" + request.getDistrictName() + request.getStateName());
                return locationInfoRepo.insert(request.getStateName(), request.getDistrictName(), request.getVillageName())
                    .flatMap(location -> generateId().flatMap(id -> persistAndWelcomeUser(id, request, location.getLocationId())));
            }));
    }

    public Mono<String> createUser(UserSignUpDTO request){
        if (request.getEmail() != null && request.getOtp() != null) {
            boolean valid = otpService.verifyRegistrationOtp(request.getEmail(), request.getOtp());
            if (!valid) {
                return Mono.just("INVALID_OTP");
            }
        }
        return createUserFromDTO(request).map(UserSearchResult::getUserId);
    }

    public Mono<String> generateId0(){
        return userInfoRepo.findlastuser()
        .doOnNext(u -> System.out.println(u))
        .map(user -> {
            System.out.println("in generate:" + user.getUserId() + user.getAge());
            String id = user.getUserId();
            System.out.println(id);
            int num = Integer.parseInt(id.replaceAll("[^0-9]", ""));
            return String.format("AAA%03d", num + 1);
        })
        .defaultIfEmpty("AAA000");
    }

    public Mono<String> generateId(){
        return idTableRepo.getUserid()
        .flatMap(id -> numberToCode(id));
    }

    private Mono<String> numberToCode(Long num) {
        return Mono.fromSupplier(() -> {
            int lettersIndex = (int) (num / 1000);
            int digits = (int) (num % 1000);

            char first = (char) ('A' + (lettersIndex / (26 * 26)) % 26);
            char second = (char) ('A' + (lettersIndex / 26) % 26);
            char third = (char) ('A' + lettersIndex % 26);

            return String.format("%c%c%c%03d", first, second, third, digits);
        });
    }
}
