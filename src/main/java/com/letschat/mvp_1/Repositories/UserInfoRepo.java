package com.letschat.mvp_1.Repositories;

import java.time.LocalDateTime;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.DTOs.UserSearchResult;
import com.letschat.mvp_1.Models.UserInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserInfoRepo extends ReactiveCrudRepository<UserInfo, String>{
    Mono<UserInfo> findByPublicName(String PublicName);

    @Query("insert into \"UserInfo\"(\"UserId\",\"PrivateName\",\"PublicName\",\"Password\",\"Email\") values(:UserId,:PrivateName,LOWER(:PublicName),:Password,:Email) returning *")
    Mono<UserInfo> insert(String UserId, String PrivateName, String PublicName, String Password, String Email);
    
    @Query("select * from \"UserInfo\" order by \"UserId\" desc limit 1")
    Mono<UserInfo> findlastuser();

    @Query("select * from \"UserInfo\" where \"UserId\"=:UserId")
    Mono<UserInfo> findByUserId(String UserId);

    @Query("select * from \"UserInfo\" where \"UserId\"=:id")
    @Override
    Mono<UserInfo> findById(String id);

    @Query("select exists(select 1 from \"UserInfo\" where \"UserId\"=:id)")
    @Override
    Mono<Boolean> existsById(String id);

    @Query("select * from \"UserInfo\"")
    @Override
    Flux<UserInfo> findAll();

    @Query("update \"UserInfo\" set \"UserProfilePath\"=:Filename where \"UserId\"=:UserId ")
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

        @Query("""
            select * from "UserInfo" where "UserId"=:id
            """)
            Mono<UserInfo> getme(String id);

        @Query("""
            update "UserInfo" set "RoomNickName"=:name where "UserId"=:id returning *
            """)
            Mono<UserInfo> setroomname(String name,String id);
            
            @Query("""
                select "RoomNickName" from "UserInfo" where "UserId"=:id
                """)
                Mono<String> getroomname(String id);

        @Query("select * from \"UserInfo\" where \"Email\"=:email limit 1")
        Mono<UserInfo> findByEmail(String email);

        @Query("select * from \"UserInfo\" where \"UserId\"=:identifier or \"Email\"=:identifier or LOWER(\"PublicName\")=LOWER(:identifier) limit 1")
        Mono<UserInfo> findByIdentifier(String identifier);

        @Query("insert into \"UserInfo\"(\"UserId\",\"PrivateName\",\"PublicName\",\"Password\",\"Email\") values(:UserId,:PrivateName,LOWER(:PublicName),:Password,:Email) returning *")
        Mono<UserInfo> insertWithEmail(String UserId, String PrivateName, String PublicName, String Password, String Email);

        @Query("update \"UserInfo\" set \"Password\"=:newPassword where \"UserId\"=:identifier or \"Email\"=:identifier returning *")
        Mono<UserInfo> updatePassword(String identifier, String newPassword);

        @Query("update \"UserInfo\" set \"Is2FAEnabled\"=:enabled where \"UserId\"=:userId or \"Email\"=:userId returning *")
        Mono<UserInfo> update2FAStatus(String userId, boolean enabled);

        @Query("""
            UPDATE "UserInfo"
            SET "PrivateName" = :displayName,
                "LastDisplayNameChangeAt" = :now
            WHERE "UserId" = :userId
              AND ("LastDisplayNameChangeAt" IS NULL OR "LastDisplayNameChangeAt" <= :cooldownThreshold)
            RETURNING *
            """)
        Mono<UserInfo> updateDisplayNameAtomic(String displayName, LocalDateTime now, LocalDateTime cooldownThreshold, String userId);

        @Query("""
            SELECT EXISTS (
                SELECT 1 FROM "UserInfo" 
                WHERE LOWER("PublicName") = LOWER(:name)
                  AND (:excludeUserId IS NULL OR "UserId" != :excludeUserId)
            )
            """)
        Mono<Boolean> existsExactName(String name, String excludeUserId);

        @Query("""
            SELECT DISTINCT "PublicName" AS name FROM "UserInfo" 
            WHERE (LOWER("PublicName") LIKE :prefix1 || '%' OR LOWER("PublicName") LIKE :prefix2 || '%')
              AND LENGTH("PublicName") BETWEEN :minLen AND :maxLen
              AND (:excludeUserId IS NULL OR "UserId" != :excludeUserId)
            """)
        Flux<String> findSimilarCandidates(String prefix1, String prefix2, int minLen, int maxLen, String excludeUserId);

    @Query("SELECT * FROM \"UserInfo\" WHERE \"BookingSlug\" = :slug OR \"UserId\" = :slug OR LOWER(\"PublicName\") = LOWER(:slug) LIMIT 1")
    Mono<UserInfo> findByBookingSlugOrUser(String slug);

    @Modifying
    @Query("UPDATE \"UserInfo\" SET \"ShowBusySlotsExternal\" = :showBusySlots WHERE \"UserId\" = :userId")
    Mono<Integer> updateShowBusySlots(String userId, Boolean showBusySlots);

    @Modifying
    @Query("UPDATE \"UserInfo\" SET \"IsBookingEnabled\" = :isBookingEnabled WHERE \"UserId\" = :userId")
    Mono<Integer> updateIsBookingEnabled(String userId, Boolean isBookingEnabled);
}
