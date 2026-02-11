package com.letschat.mvp_1.Repositories;

import java.time.LocalDateTime;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.MessageTrackHistory;

import io.micrometer.common.lang.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageTrackHistoryRepo extends ReactiveCrudRepository<MessageTrackHistory, Long>{
    @Query("""
            insert into "MessageTrackHistory" ("MessageId","SenderId","RecieverId","Status","DeliverTime")
            values(:msgid,:senderid,:recieverid,:status,:delivertime) returning *
            """)
    Mono<MessageTrackHistory> insert(String msgid, String senderid, String recieverid, String status, @Nullable
            LocalDateTime delivertime);

     @Query("""
          update "MessageTrackHistory" 
          set "Status"='read' ,"ReadTime"=:time,"DeliverTime"=:time where ("Status"='pending' or "Status"='delivered')
and "MessageId"=:msgid
and "RecieverId"=:userid returning *
          """)
    Mono<MessageTrackHistory> updatestatusonchat(String msgid,String userid,LocalDateTime time);

    @Query("""
            update "MessageTrackHistory" 
            set "Status"='delivered' ,"DeliverTime"=:time where "Status"='pending'
            and "RecieverId"=:userid returning *
            """)
    Flux<MessageTrackHistory> updatestatusonconnect(String userid,LocalDateTime time);


    @Query("""
                    select uc."ChatId" from "UserChat" uc join "MessageInfo" mi on uc."Id"=mi."ChatId"
                    where "MessageId"=:msgid
                    """)
        Mono<String> getchatidfrommsgid(String msgid);


        @Query("""
update "MessageTrackHistory" mt set "Status"='read',"ReadTime"=:time from "MessageInfo" mc join (select * from "UserChat" 
where "ChatId"=:chatid order by "Id" asc limit 1) uc on mc."ChatId"=uc."Id" 
where mt."MessageId"=mc."MessageId" and mt."RecieverId"=:userid and (mt."Status"='delivered' or mt."Status"='pending') and mc."SpaceId"=:space
 returning *
                        """)
        Flux<MessageTrackHistory> updateoncheckin(String userid,String chatid,LocalDateTime time,Integer space);

        @Query("""
                        UPDATE "MessageTrackHistory"
                        SET 
                        "DeleteForSender" = CASE WHEN "SenderId" = :userId THEN TRUE ELSE "DeleteForSender" END,
                        "DeleteForReceiver" = CASE WHEN "RecieverId" = :userId THEN TRUE ELSE "DeleteForReceiver" END
                        WHERE "MessageId" = :msgId
                        AND (:userId = "SenderId" OR :userId = "RecieverId") returning *
                        """)
        Flux<MessageTrackHistory> deletemessage(String userId,String msgId);

        @Query("""
                      UPDATE "MessageTrackHistory"
                        SET 
                        "DeleteForSender" = true,
                        "DeleteForReceiver" = true
                        WHERE "MessageId" = :msgId
                        AND (:userId = "SenderId" OR :userId = "RecieverId") returning * 
                        
                        """)
        Flux<MessageTrackHistory> deleteforeone(String userId,String msgId);

        @Query("""
                        UPDATE "MessageTrackHistory"
                        SET 
                        "DeleteForSender" = CASE WHEN "SenderId" = :userId THEN false ELSE "DeleteForSender" END,
                        "DeleteForReceiver" = CASE WHEN "RecieverId" = :userId THEN false ELSE "DeleteForReceiver" END
                        WHERE "MessageId" = :msgId
                        AND (:userId = "SenderId" OR :userId = "RecieverId") returning *
                        """)
        Flux<MessageTrackHistory> reviveforme(String userId,String msgId);


        @Query("""
                      UPDATE "MessageTrackHistory"
                        SET 
                        "DeleteForSender" = false,
                        "DeleteForReceiver" = false
                        WHERE "MessageId" = :msgId
                        AND (:userId = "SenderId" OR :userId = "RecieverId") returning *  
                        """)
        Flux<MessageTrackHistory> reviveforeone(String userId,String msgId);

                @Query("""
                       select * from "MessageTrackHistory" where "MessageId"=:msgId 
                       """)
                       Flux<MessageTrackHistory> getmsgid(String msgId);
}
