package com.letschat.mvp_1.Repositories;

import java.time.LocalDateTime;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.DTOs.ChatBoxReturnDTO;
import com.letschat.mvp_1.DTOs.SenderInfoDTO;
import com.letschat.mvp_1.Models.UserChatInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface UserChatInfoRepo extends ReactiveCrudRepository<UserChatInfo, Long>{
     @Query("select * from \"UserChat\" where \"Id\"=:id")
    Mono<UserChatInfo>  findByChatId(Long Id);

    @Query("select * from \"UserChat\" order by \"ChatId\" desc limit 1")
    Mono<UserChatInfo> findlastuser();

    @Query("INSERT INTO \"UserChat\" (\"ChatId\", \"UserId\",\"ChatName\", \"UserName\", \"At\", \"Visited\",\"Type\",\"Role\",\"ChatProfile\",\"UserProfile\",\"IsMuted\",\"IsCustomName\",\"IsCustomProfile\") VALUES (:ChatId, :UserId, :ChatName, :UserName, :At, :Visited,:Type,:Role,:ChatProfile,:UserProfile, false, false, false) returning *")
    Mono<UserChatInfo> insert(String ChatId,String UserId,String ChatName,String UserName,LocalDateTime At,LocalDateTime Visited,String Type,String Role,String ChatProfile,String UserProfile);

    @Query("INSERT INTO \"UserChat\" (\"ChatId\", \"UserId\",\"ChatName\", \"UserName\", \"At\", \"Visited\",\"Type\",\"Role\",\"ChatProfile\",\"UserProfile\",\"IsMuted\",\"IsCustomName\",\"IsCustomProfile\") VALUES (:ChatId, :UserId, :ChatName, :UserName, :At, :Visited,:Type,:Role,:ChatProfile,:UserProfile, COALESCE(:IsMuted, false), false, false) returning *")
    Mono<UserChatInfo> insertWithMute(String ChatId,String UserId,String ChatName,String UserName,LocalDateTime At,LocalDateTime Visited,String Type,String Role,String ChatProfile,String UserProfile,Boolean IsMuted);

    @Query("select * from \"UserChat\" where \"UserId\"=:UserId and (\"ChatName\"=:UserName1 or \"ChatName\"=:UserName2) limit 1")
    Mono<UserChatInfo> findIfExist(String UserId,String UserName1,String UserName2);

    @Query("""
            select uc1."ChatId" as ChatId,ui."UserId" as Id,uc1."ChatName" as ChatName,uc1."Type" as Type,uc1."Status" as Status,uc1."At" as timestamp,uc1."ChatProfile" as "Profile", COALESCE(uc1."IsMuted", false) as isMuted, ui."PublicName" as username, ui."PrivateName" as displayName from "UserChat" uc1 join "UserChat" uc2 on uc1."ChatId"=uc2."ChatId"
join "UserInfo" ui on ui."UserId"=uc2."UserId" where uc1."UserId"=:UserId and uc2."UserId"!=:UserId and uc1."Type"='private'
            """)
    Flux<ChatBoxReturnDTO> findUserChat(String UserId);

    @Query("""
select user_chats."ChatId" as ChatId,gi."GroupId" as Id,gi."GroupName" as ChatName,gi."GroupProfilePath" as "Profile",gi."Motto" as motto,user_chats."Type" as Type,user_chats."Status" as Status,user_chats."At" as timestamp,user_chats."Role" as Role, COALESCE(user_chats."IsMuted", false) as isMuted from (select "ChatId","Type","At","Role","Status","IsMuted" from "UserChat" where "UserId"=:UserId )As user_chats join(select "ChatId",MIN("Id") as min_id from "UserChat" group by "ChatId" )
As chat_min_ids on user_chats."ChatId"=chat_min_ids."ChatId" join "GroupInfo" gi on gi."ChatId"=chat_min_ids.min_id            """)
    Flux<ChatBoxReturnDTO> findGroupChat(String UserId);

    @Query("""
select user_chats."ChatId" as ChatId,gi."room_id" as Id,gi."room_name" as ChatName,gi."room_profile" as "Profile",gi."description" as motto,gi."description" as description,user_chats."Type" as Type,user_chats."Status" as Status,user_chats."At" as timestamp,user_chats."Role" as Role, COALESCE(user_chats."IsMuted", false) as isMuted from (select "ChatId","Type","At","Role","Status","IsMuted" from "UserChat" where "UserId"=:UserId )As user_chats join(select "ChatId",MIN("Id") as min_id from "UserChat" group by "ChatId" )
As chat_min_ids on user_chats."ChatId"=chat_min_ids."ChatId" join "ClassRoomInfo" gi on gi."chat_id"=chat_min_ids.min_id           
            """)
            Flux<ChatBoxReturnDTO> findClassRoomChat(String UserId);

    //@Query("select \"UserId\" from \"UserChat\" where \"ChatId\"=:ChatId and \"UserId\"!=:UserId and \"Status\"='allowed'")
    @Query("select \"UserId\" from \"UserChat\" where \"ChatId\"=:ChatId and \"Status\"='allowed'")
    Flux<String> findUserIds(String ChatId);

    @Query("""
            select "Type" from "UserChat" where "ChatId"=:chatid limit 1
            """)
            Mono<String>findTypeByChatId(String chatid);

        @Query("""
            select "ChatName" from "UserChat" where "ChatId"=:chatid limit 1
            """)
            Mono<String>findChatNameByChatId(String chatid);

        @Query("""
                        select "ChatId" from "UserChat" where "UserId" IN (:U1,:U2) and "Type"='private'
group by "ChatId" having count(distinct "UserId")=2
                        """)
        Mono<String> findIfExistOrNot(String U1,String U2);


        @Query("""
                        select "ChatId" from "UserChat" where "ChatId"=:C1 and "UserId"=:U1

                        """)
        Mono<String> joinedOrNot(String C1,String U1);

       @Query("""
            select * from "UserChat" where "ChatId"=:chatid and "UserId"=:userid and "Status"='allowed'
            """)
            Mono<UserChatInfo> getInfo(String chatid,String userid);


        @Query("""
                update "UserChat" set "UserName"=:name, "UserProfile"=:Profile where "ChatId"=:chatid and "UserId"=:userid returning *
                        """)
        Mono<UserChatInfo> updateUsername(String name,String Profile,String chatid,String userid);

        @Query("""
                UPDATE "UserChat" 
                SET "UserName" = COALESCE(:name, "UserName"), 
                    "UserProfile" = COALESCE(:profile, "UserProfile"),
                    "IsCustomName" = CASE WHEN :isCustomName IS NOT NULL THEN :isCustomName ELSE "IsCustomName" END,
                    "IsCustomProfile" = CASE WHEN :isCustomProfile IS NOT NULL THEN :isCustomProfile ELSE "IsCustomProfile" END
                WHERE "ChatId" = :chatId AND "UserId" = :userId
                RETURNING *
                """)
        Mono<UserChatInfo> updateCustomChatInfo(String name, String profile, Boolean isCustomName, Boolean isCustomProfile, String chatId, String userId);

        @Modifying
        @Query("""
                UPDATE "UserChat"
                SET "UserName" = :newDisplayName
                WHERE "UserId" = :userId
                  AND ("IsCustomName" IS FALSE OR "IsCustomName" IS NULL)
                  AND NOT EXISTS (
                      SELECT 1 FROM "UserChat" uc2 
                      WHERE uc2."ChatId" = "UserChat"."ChatId" 
                        AND uc2."UserId" != :userId 
                        AND LOWER(REGEXP_REPLACE(uc2."UserName", '\\s+', '', 'g')) = LOWER(REGEXP_REPLACE(:newDisplayName, '\\s+', '', 'g'))
                        AND (uc2."Status" = 'allowed' OR uc2."Status" IS NULL)
                  )
                """)
        Mono<Integer> propagateDisplayName(String userId, String newDisplayName);

        @Modifying
        @Query("""
                UPDATE "UserChat"
                SET "UserProfile" = :newProfilePath
                WHERE "UserId" = :userId
                  AND ("IsCustomProfile" IS FALSE OR "IsCustomProfile" IS NULL)
                """)
        Mono<Integer> propagateProfilePicture(String userId, String newProfilePath);

        @Query("""
                SELECT "UserName" FROM "UserChat" 
                WHERE "ChatId" = :chatId AND "UserId" != :excludeUserId AND ("Status" = 'allowed' OR "Status" IS NULL)
                """)
        Flux<String> findPeerUserNames(String chatId, String excludeUserId);

        @Query("""
                update "UserChat" set "ChatName"=:name,"ChatProfile"=:Profile where "ChatId"=:chatid and "UserId"!=:userid returning *
                        """)
        Mono<UserChatInfo> updateChatname(String name,String Profile,String chatid,String userid);

        @Query("""
                update "UserChat" set "Status"=:action where "ChatId"=:chatid and "UserId"=:userid returning *
                        """)
        Mono<UserChatInfo> updateStatus(String action,String chatid,String userid);

         @Query("""
                        select * from "UserChat" where "ChatId"=:C1 and "UserId"=:U1

                        """)
        Mono<UserChatInfo> getUserChat(String C1,String U1);

        @Query("""
                select "UserName" from "UserChat" where "ChatId"=:cid and "UserId"=:uid
                        """)
                Mono<String> getUsername(String cid,String uid);

        @Query("""
                SELECT DISTINCT uc2."UserId"
                FROM "UserChat" uc1
                JOIN "UserChat" uc2 ON uc1."ChatId" = uc2."ChatId"
                WHERE uc1."UserId" = :userId AND uc2."UserId" != :userId AND uc1."Status" = 'allowed'
                """)
        Flux<String> findContactUserIds(String userId);

        @Query("""
                SELECT * FROM "UserChat"
                WHERE "ChatId" = :chatId AND "Status" = 'allowed' AND ("Role" != 'faculty' OR "Role" IS NULL)
                """)
        Flux<UserChatInfo> findClassroomStudents(String chatId);

        @Modifying
        @Query("""
                UPDATE "UserChat" 
                SET "IsMuted" = :isMuted 
                WHERE "ChatId" = :chatId AND "UserId" = :userId
                """)
        Mono<Integer> updateMuteStatus(String chatId, String userId, Boolean isMuted);

        @Query("""
                SELECT COALESCE("IsMuted", false) 
                FROM "UserChat" 
                WHERE "ChatId" = :chatId AND "UserId" = :userId 
                LIMIT 1
                """)
        Mono<Boolean> isChatMuted(String chatId, String userId);

        @Query("""
                SELECT 
                    mi."SenderId" AS sender_id,
                    COALESCE(
                        (
                            SELECT uc2."UserName"
                            FROM "UserChat" uc1
                            JOIN "UserChat" uc2 
                              ON uc2."ChatId" = uc1."ChatId" 
                             AND uc2."UserId" = mi."SenderId"
                            WHERE uc1."Id" = mi."ChatId"
                            LIMIT 1
                        ),
                        'Member'
                    ) AS sender_name
                FROM "MessageInfo" mi
                WHERE mi."MessageId" = :msgId
                LIMIT 1
                """)
        Mono<SenderInfoDTO> findSenderInChat(String msgId);
}
