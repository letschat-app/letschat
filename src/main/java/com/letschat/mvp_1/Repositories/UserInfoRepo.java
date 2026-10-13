package com.letschat.mvp_1.Repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.DTOs.UserSearchResult;
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

        @Query("select * from \"UserInfo\" where \"UserId\"=:identifier or \"Email\"=:identifier limit 1")
        Mono<UserInfo> findByIdentifier(String identifier);

        @Query("insert into \"UserInfo\"(\"UserId\",\"PrivateName\",\"PublicName\",\"Age\",\"Gender\",\"Location\",\"Password\",\"Email\") values(:UserId,:PrivateName,:PublicName,:Age,:Gender,:Location,:Password,:Email) returning *")
        Mono<UserInfo> insertWithEmail(String UserId, String PrivateName, String PublicName, int Age, String Gender, long Location, String Password, String Email);

        @Query("update \"UserInfo\" set \"Password\"=:newPassword where \"UserId\"=:identifier or \"Email\"=:identifier returning *")
        Mono<UserInfo> updatePassword(String identifier, String newPassword);

        @Query("update \"UserInfo\" set \"Is2FAEnabled\"=:enabled where \"UserId\"=:userId or \"Email\"=:userId returning *")
        Mono<UserInfo> update2FAStatus(String userId, boolean enabled);

        @Query("""
            SELECT EXISTS (
                SELECT 1 FROM "UserInfo" 
                WHERE (LOWER("PublicName") = :name OR LOWER("PrivateName") = :name OR LOWER("RoomNickName") = :name)
                  AND (:excludeUserId IS NULL OR "UserId" != :excludeUserId)
                UNION ALL
                SELECT 1 FROM "UserChat" 
                WHERE LOWER("UserName") = :name
                  AND (:excludeUserId IS NULL OR "UserId" != :excludeUserId)
            )
            """)
        Mono<Boolean> existsExactName(String name, String excludeUserId);

        @Query("""
            SELECT DISTINCT name FROM (
                SELECT "PublicName" AS name FROM "UserInfo" 
                WHERE (LOWER("PublicName") LIKE :prefix1 || '%' OR LOWER("PublicName") LIKE :prefix2 || '%')
                  AND LENGTH("PublicName") BETWEEN :minLen AND :maxLen
                  AND (:excludeUserId IS NULL OR "UserId" != :excludeUserId)

                UNION

                SELECT "PrivateName" AS name FROM "UserInfo" 
                WHERE (LOWER("PrivateName") LIKE :prefix1 || '%' OR LOWER("PrivateName") LIKE :prefix2 || '%')
                  AND LENGTH("PrivateName") BETWEEN :minLen AND :maxLen
                  AND (:excludeUserId IS NULL OR "UserId" != :excludeUserId)

                UNION

                SELECT "RoomNickName" AS name FROM "UserInfo" 
                WHERE "RoomNickName" IS NOT NULL 
                  AND (LOWER("RoomNickName") LIKE :prefix1 || '%' OR LOWER("RoomNickName") LIKE :prefix2 || '%')
                  AND LENGTH("RoomNickName") BETWEEN :minLen AND :maxLen
                  AND (:excludeUserId IS NULL OR "UserId" != :excludeUserId)

                UNION

                SELECT "UserName" AS name FROM "UserChat" 
                WHERE "UserName" IS NOT NULL 
                  AND (LOWER("UserName") LIKE :prefix1 || '%' OR LOWER("UserName") LIKE :prefix2 || '%')
                  AND LENGTH("UserName") BETWEEN :minLen AND :maxLen
                  AND (:excludeUserId IS NULL OR "UserId" != :excludeUserId)
            ) AS candidate_names
            """)
        Flux<String> findSimilarCandidates(String prefix1, String prefix2, int minLen, int maxLen, String excludeUserId);
}
