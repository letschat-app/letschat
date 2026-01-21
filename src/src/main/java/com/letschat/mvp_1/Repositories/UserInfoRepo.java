package com.letschat.mvp_1.Repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.UserInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserInfoRepo extends ReactiveCrudRepository<UserInfo, String>{
    Mono<UserInfo> findByPublicName(String PublicName);

    @Query("insert into \"UserInfo\"(\"UserId\",\"PrivateName\",\"PublicName\",\"Age\",\"Gender\",\"Location\",\"Password\")values(:UserId,:PrivateName,:PublicName,:Age,:Gender,:Location,:Password)")
    Mono<UserInfo> insert(String UserId,String PrivateName,String PublicName,int Age,String Gender,long Location,String Password);
    
    @Query("select * from \"UserInfo\" order by \"UserId\" desc limit 1")
    Mono<UserInfo> findlastuser();

    @Query("select * from \"UserInfo\" where \"UserId\"=:UserId")
    Mono<UserInfo> findByUserId(String UserId);

    @Query("select * from \"UserInfo\"")
    @Override
    Flux<UserInfo> findAll();

    @Query("update  \"UserInfo\" set \"UserProfilePath\"=:Filename where \"UserId\"=:UserId ")
    Mono<UserInfo> updateprofile(String Filename,String UserId);

    @Query("select \"PublicName\" from \"UserInfo\" where \"UserId\"=:UserId")
    Mono<String> findPublicName(String UserId);

    @Query("""
            SELECT 
      CASE 
        WHEN uc."Type" = 'private' OR uc."Type" = 'group' 
        THEN ui."PrivateName"
        ELSE ui."PublicName"
      END
    FROM "UserChat" uc
    JOIN "UserInfo" ui ON ui."UserId" = :Userid
    WHERE uc."ChatId" = :Chatid
	limit 1
            """)
        Mono<String> findName(String Userid,String Chatid);
    
}
