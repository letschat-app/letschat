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

    @Query("INSERT INTO \"UserChat\" (\"ChatId\", \"UserId\",\"ChatName\", \"UserName\", \"At\", \"Visited\",\"Type\") VALUES (:ChatId, :UserId, :ChatName, :UserName, :At, :Visited,:Type) returning *")
    Mono<UserChatInfo> insert(String ChatId,String UserId,String ChatName,String UserName,LocalDateTime At,LocalDateTime Visited,String Type);

    @Query("select * from \"UserChat\" where \"UserId\"=:UserId and (\"ChatName\"=:UserName1 or \"ChatName\"=:UserName2) limit 1")
    Mono<UserChatInfo> findIfExist(String UserId,String UserName1,String UserName2);

    @Query("""
            select uc1."ChatId" as ChatId,ui."UserId" as Id,ui."PublicName" as ChatName,uc1."Type" as Type from "UserChat" uc1 join "UserChat" uc2 on uc1."ChatId"=uc2."ChatId"
join "UserInfo" ui on ui."UserId"=uc2."UserId" where uc1."UserId"=:UserId and uc2."UserId"!=:UserId and uc1."Type"='private'
            """)
    Flux<ChatBoxReturnDTO> findUserChat(String UserId);

    @Query("""

select user_chats."ChatId" as ChatId,gi."GroupId" as Id,gi."GroupName" as ChatName,user_chats."Type" as Type from (select "ChatId","Type" from "UserChat" where "UserId"=:UserId )As user_chats join(select "ChatId",MIN("Id") as min_id from "UserChat" group by "ChatId" )
As chat_min_ids on user_chats."ChatId"=chat_min_ids."ChatId" join "GroupInfo" gi on gi."ChatId"=chat_min_ids.min_id            """)
    Flux<ChatBoxReturnDTO> findGroupChat(String UserId);

    @Query("select \"UserId\" from \"UserChat\" where \"ChatId\"=:ChatId and \"UserId\"!=:UserId")
    Flux<String> findUserIds(String ChatId,String UserId);
}
