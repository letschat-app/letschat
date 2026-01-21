package com.letschat.mvp_1.Repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

//import com.letschat.mvp_1.DTOs.UserSearchResult;
import com.letschat.mvp_1.Models.GroupInfoModel;
import com.letschat.mvp_1.Models.UserChatInfo;


import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GroupInfoRepo extends ReactiveCrudRepository<GroupInfoModel, String>{
    
    @Query("insert into \"GroupInfo\" (\"GroupId\",\"GroupName\",\"CreatedBy\",\"ChatId\")values(:GroupId,:GroupName,:CreatedBy,:ChatId) returning *")
    Mono<GroupInfoModel> insert(String GroupId,String GroupName,String CreatedBy,Long ChatId);

    @Query("select * from \"GroupInfo\" order by \"GroupId\" desc limit 1")
    Mono<GroupInfoModel> findlastuser();

    @Query("select * from \"GroupInfo\" where \"GroupId\"=:GroupId")
    Mono<GroupInfoModel> findByGroupId(String GroupId);

    @Query("update \"GroupInfo\" set \"NoOfMembers\"=:members where \"GroupId\"=:groupid")
    Mono<GroupInfoModel> updatemembers(String groupid,Integer members);
//not used
    @Query("""
            select * from "GroupInfo" where "ChatId"=:chatid
            """)
            Flux<GroupInfoModel> get(String chatid);

    @Query("""
            select * from "UserChat" where "ChatId"=:chatid and "Status"='allowed'
            """)
            Flux<UserChatInfo> getMembers(String chatid);

    @Query("""
            update "UserChat" set "Role"='admin' where "ChatId"=:chatid and "UserId"=:userid
            """)
            Mono<UserChatInfo> makeAdmin(String chatid,String userid);

    @Query("""
            update "UserChat" set "Status"=:status where "ChatId"=:chatid and "UserId"=:userid
            """)
            Mono<UserChatInfo> removeOrLeaveGroup(String status,String chatid,String userid);

    
}

