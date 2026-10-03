package com.letschat.mvp_1.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<String, UserSignUpDTO> pendingSignupMap = new ConcurrentHashMap<>();

    public UserSignUpService(UserInfoRepo userInfoRepo, LocationInfoRepo locationInfoRepo, IdTableRepo idTableRepo, AdminService adminService, OtpService otpService){
        this.userInfoRepo = userInfoRepo;
        this.locationInfoRepo = locationInfoRepo;
        this.idTableRepo = idTableRepo;
        this.adminService = adminService;
        this.otpService = otpService;
    }   

    public Mono<Boolean> initSignup(UserSignUpDTO request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return Mono.just(false);
        }
        String cleanEmail = request.getEmail().trim().toLowerCase();
        pendingSignupMap.put(cleanEmail, request);
        return otpService.sendRegistrationOtp(cleanEmail);
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

        UserSignUpDTO request = pendingSignupMap.remove(cleanEmail);
        if (request == null) {
            return Mono.empty();
        }

        return createUserFromDTO(request);
    }

    public Mono<UserSearchResult> createUserFromDTO(UserSignUpDTO request) {
        return locationInfoRepo.findExact(request.getStateName(), request.getDistrictName(), request.getVillageName())
        .flatMap(location -> generateId().flatMap(id -> {
            System.out.println("flat");
            return userInfoRepo.insertWithEmail(id, request.getPrivateName(), request.getPublicName(), request.getAge(), request.getGender(), location.getLocationId(), request.getPassword(), request.getEmail())
            .doOnSuccess(msg -> adminService.onUserRegistered(id))
            .map(user -> {
                UserSearchResult result = new UserSearchResult();
                result.setUserId(id);
                result.setUserName(user.getPublicName());
                result.setProfile(user.getUserProfilePath());
                return result;
            });
        }))
        .switchIfEmpty(Mono.defer(() -> {
            System.out.println("switch:" + request.getDistrictName() + request.getStateName());
            LocationInfo locationInfo = new LocationInfo();
            locationInfo.setStateName(request.getStateName());
            locationInfo.setDistrictName(request.getDistrictName());
            locationInfo.setVillageName(request.getVillageName());
            return locationInfoRepo.insert(request.getStateName(), request.getDistrictName(), request.getVillageName())
            .flatMap(location -> generateId().flatMap(id -> {
                System.out.println(location.getLocationId());
                return userInfoRepo.insertWithEmail(id, request.getPrivateName(), request.getPublicName(), request.getAge(), request.getGender(), location.getLocationId(), request.getPassword(), request.getEmail())
                .doOnSuccess(msg -> adminService.onUserRegistered(id))
                .map(user -> {
                    UserSearchResult result = new UserSearchResult();
                    result.setUserId(id);
                    result.setUserName(user.getPublicName());
                    result.setProfile(user.getUserProfilePath());
                    return result;
                });
            }));
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
