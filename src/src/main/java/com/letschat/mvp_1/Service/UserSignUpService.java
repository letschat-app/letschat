package com.letschat.mvp_1.Service;

import org.springframework.stereotype.Component;

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

    public UserSignUpService(UserInfoRepo userInfoRepo,LocationInfoRepo locationInfoRepo,IdTableRepo idTableRepo){
        this.userInfoRepo=userInfoRepo;
        this.locationInfoRepo=locationInfoRepo;
        this.idTableRepo=idTableRepo;
    }   


    public Mono<String> createUser(UserSignUpDTO request){
        // locationInfo.setStateName(request.getStateName());
        // locationInfo.setDistrictName(request.getDistrictName());
        // locationInfo.setVillageName(request.getVillageName());
        return locationInfoRepo.findExact(request.getStateName(), request.getDistrictName(), request.getVillageName())
        .flatMap(location->generateId().flatMap(id->{
            // userInfo.setUserId(id);
            // userInfo.setLocation(location.getLocationId());
           // return userInfoRepo.save(userInfo).thenReturn("user created");
           System.out.println("flat");
            return userInfoRepo.insert(id, request.getPrivateName(), request.getPublicName(), request.getAge(), request.getGender(), location.getLocationId(), request.getPassword())
            .thenReturn(id);
        }))
        .switchIfEmpty(Mono.defer(()->{
            System.out.println("switch:"+request.getDistrictName()+request.getStateName());
            LocationInfo locationInfo=new LocationInfo();
            locationInfo.setStateName(request.getStateName());
            locationInfo.setDistrictName(request.getDistrictName());
            locationInfo.setVillageName(request.getVillageName());
            return locationInfoRepo.insert(request.getStateName(), request.getDistrictName(), request.getVillageName())
            .flatMap(location->generateId().flatMap(id->{
                System.out.println(location.getLocationId());
                 return userInfoRepo.insert(id, request.getPrivateName(), request.getPublicName(), request.getAge(), request.getGender(), location.getLocationId(), request.getPassword())
            .thenReturn(id);
            }));
        })
        );


        // .flatMap(location->{
        //     userInfo.setUserId(id);
        //     userInfo.setPrivateName(request.getPrivateName());
        //     userInfo.setPublicName(request.getPublicName());
        //     userInfo.setAge(request.getAge());
        //     userInfo.setGender(request.getGender());
        //     userInfo.setLocation(location.getLocationId());
        //     userInfo.setPassword(request.getPassword());
        //     return UserInfoRepo.save(userInfo).thenReturn(ResponseEntity.status(201).body("user created"));
        // })
        // .switchIfEmpty(alternate)
        

       
    }
    public Mono<String> generateId0(){
        return userInfoRepo.findlastuser()
        .doOnNext(u->System.out.println(u))
        .map(user->{
            System.out.println("in generate:"+user.getUserId()+user.getAge());
            String id=user.getUserId();//AAA000
            System.out.println(id);
            int num =Integer.parseInt(id.replaceAll("[^0-9]", ""));
            return String.format("AAA%03d", num+1);
        })
        .defaultIfEmpty("AAA000");
    }

    public Mono<String> generateId(){
        return idTableRepo.getUserid()
        .flatMap(id->{
            return numberToCode(id);
        });
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
