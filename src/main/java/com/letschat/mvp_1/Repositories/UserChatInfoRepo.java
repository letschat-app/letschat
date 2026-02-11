package com.letschat.mvp_1.Repositories;

import java.time.LocalDateTime;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.DTOs.ChatBoxReturnDTO;
import com.letschat.mvp_1.Models.UserChatInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface UserChatInfoRepo extends ReactiveCrudRepository<UserChatInfo, Long>{
     @Query("select * from \"UserChat\" where \"Id\"=:id")
    Mono<UserChatInfo>  findByChatId(Long Id);

    @Query("select * from \"UserChat\" order by \"ChatId\" desc limit 1")
    Mono<UserChatInfo> findlastuser();

    @Query("INSERT INTO \"UserChat\" (\"ChatId\", \"UserId\",\"ChatName\", \"UserName\", \"At\", \"Visited\",\"Type\",\"Role\") VALUES (:ChatId, :UserId, :ChatName, :UserName, :At, :Visited,:Type,:Role) returning *")
    Mono<UserChatInfo> insert(String ChatId,String UserId,String ChatName,String UserName,LocalDateTime At,LocalDateTime Visited,String Type,String Role);

    @Query("select * from \"UserChat\" where \"UserId\"=:UserId and (\"ChatName\"=:UserName1 or \"ChatName\"=:UserName2) limit 1")
    Mono<UserChatInfo> findIfExist(String UserId,String UserName1,String UserName2);

    @Query("""
            select uc1."ChatId" as ChatId,ui."UserId" as Id,uc1."ChatName" as ChatName,uc1."Type" as Type,uc1."Status" as Status,uc1."At" as timestamp from "UserChat" uc1 join "UserChat" uc2 on uc1."ChatId"=uc2."ChatId"
join "UserInfo" ui on ui."UserId"=uc2."UserId" where uc1."UserId"=:UserId and uc2."UserId"!=:UserId and uc1."Type"='private'
            """)
    Flux<ChatBoxReturnDTO> findUserChat(String UserId);

    @Query("""

select user_chats."ChatId" as ChatId,gi."GroupId" as Id,gi."GroupName" as ChatName,user_chats."Type" as Type,user_chats."Status" as Status,user_chats."At" as timestamp,user_chats."Role" as Role from (select "ChatId","Type","At","Role","Status" from "UserChat" where "UserId"=:UserId )As user_chats join(select "ChatId",MIN("Id") as min_id from "UserChat" group by "ChatId" )
As chat_min_ids on user_chats."ChatId"=chat_min_ids."ChatId" join "GroupInfo" gi on gi."ChatId"=chat_min_ids.min_id            """)
    Flux<ChatBoxReturnDTO> findGroupChat(String UserId);

    @Query("""
select user_chats."ChatId" as ChatId,gi."room_id" as Id,gi."room_name" as ChatName,user_chats."Type" as Type,user_chats."Status" as Status,user_chats."At" as timestamp,user_chats."Role" as Role from (select "ChatId","Type","At","Role","Status" from "UserChat" where "UserId"=:UserId )As user_chats join(select "ChatId",MIN("Id") as min_id from "UserChat" group by "ChatId" )
As chat_min_ids on user_chats."ChatId"=chat_min_ids."ChatId" join "ClassRoomInfo" gi on gi."chat_id"=chat_min_ids.min_id           
            """)
            Flux<ChatBoxReturnDTO> findClassRoomChat(String UserId);

    @Query("select \"UserId\" from \"UserChat\" where \"ChatId\"=:ChatId and \"UserId\"!=:UserId and \"Status\"='allowed'")
    Flux<String> findUserIds(String ChatId,String UserId);

    @Query("""
            select "Type" from "UserChat" where "ChatId"=:chatid limit 1
            """)
            Mono<String>findTypeByChatId(String chatid);

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
                update "UserChat" set "UserName"=:name where "ChatId"=:chatid and "UserId"=:userid returning *
                        """)
        Mono<UserChatInfo> updateUsername(String name,String chatid,String userid);

        @Query("""
                update "UserChat" set "ChatName"=:name where "ChatId"=:chatid and "UserId"!=:userid returning *
                        """)
        Mono<UserChatInfo> updateChatname(String name,String chatid,String userid);

        @Query("""
                update "UserChat" set "Status"=:action where "ChatId"=:chatid and "UserId"=:userid returning *
                        """)
        Mono<UserChatInfo> updateStatus(String action,String chatid,String userid);

         @Query("""
                        select * from "UserChat" where "ChatId"=:C1 and "UserId"=:U1

                        """)
        Mono<UserChatInfo> getUserChat(String C1,String U1);
}
