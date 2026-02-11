package com.letschat.mvp_1.Repositories;

import java.time.LocalDateTime;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.DTOs.LoadMessageDTO;
import com.letschat.mvp_1.Models.MessageInfo;
import com.letschat.mvp_1.Models.MessageTrackHistory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageInfoRepo extends ReactiveCrudRepository<MessageInfo, String>{
    @Query("""
            insert into "MessageInfo" ("MessageId","ChatId","SenderId","MessageType","Message","MediaId","RepliedTo","ForwardedFrom","Time","SpaceId")
values(:MessageId,(select "Id" from "UserChat" where "ChatId"=:ChatId order by "Id" asc limit 1),:SenderId,:MessageType,
case when :MessageType='text'then :Content else null end,case when :MessageType !='text'then :Content else null end,:RepliedTo,
(select "Id" from "UserChat" where "ChatId"=:ForwardedChat and "UserId"=:SenderId limit 1),:Time,:SpaceId) returning *

            """)
    Mono<MessageInfo> insert(String MessageId,String ChatId,String SenderId,String MessageType,String Content,String RepliedTo,String ForwardedChat,LocalDateTime Time,Integer SpaceId);


    @Query("""
            select * from(
SELECT distinct on (mi."MessageId")
  mi."MessageId" as msgid,
 
  (select "ChatId" from "UserChat" uc0 where uc0."Id"=mi."ChatId" limit 1) as "chatid",
  mi."MessageType" as type,
  CASE 
    WHEN mi."MessageType" = 'text' THEN mi."Message"
    ELSE mi."MediaId"
  END AS content,
  
  (
  SELECT uc2."UserName"
  FROM "UserChat" uc1
  JOIN "UserChat" uc2
    ON uc2."ChatId" = uc1."ChatId"
   AND uc2."UserId" = mi."SenderId"
  WHERE uc1."Id" = mi."ChatId"
  LIMIT 1
) AS sendername,

  mi."SenderId" as userid,
  
  mi."Time" as timestamp,
  mi."RepliedTo" as repliedto,
(select "ChatId" from "UserChat" uc1 where uc1."Id"=mi."ForwardedFrom" limit 1)as forwardedfrom,

  (SELECT CASE 
                       WHEN mt2."SenderId" = :Userid THEN mt2."Status"
                       ELSE NULL
                   END
            FROM "MessageTrackHistory" mt2
            WHERE mt2."MessageId" = mi."MessageId"
              AND (mt2."SenderId" = :Userid OR mt2."RecieverId" = :Userid)
            ORDER BY 
                CASE mt2."Status"
                    WHEN 'pending' THEN 1
                    WHEN 'delivered' THEN 2
                    WHEN 'read' THEN 3
                    ELSE 4
                END
            LIMIT 1) AS status,

            CASE
            WHEN mi."SenderId" =:Userid THEN true
            ELSE (
                SELECT CASE 
                           WHEN mt2."Status" IN ('read') THEN true
                           ELSE false
                       END
                FROM "MessageTrackHistory" mt2
                WHERE mt2."MessageId" = mi."MessageId"
                  AND mt2."RecieverId" = :Userid
                LIMIT 1
            )
        END AS isold,

        (
          SELECT 
              CASE 
                  WHEN mt2."SenderId" = :Userid THEN mt2."DeleteForSender"
                  WHEN mt2."RecieverId" = :Userid THEN mt2."DeleteForReceiver"
                  ELSE FALSE
              END
          FROM "MessageTrackHistory" mt2
          WHERE mt2."MessageId" = mi."MessageId"
            AND (:Userid = mt2."SenderId" OR :Userid = mt2."RecieverId")
          LIMIT 1
      ) AS isdeleted,

      mi."DeletedBy" as isdeletedeone,
      mi."IsRevived" as revived,
      mi."SpaceId" as spaceid

FROM "MessageInfo" mi
join "UserChat" uct on uct."ChatId"='AAA000' and uct."UserId"='AAA000'
JOIN "MessageTrackHistory" mt ON mi."MessageId" = mt."MessageId"
WHERE (mt."SenderId" = :Userid OR mt."RecieverId" = :Userid) and mi."ChatId"=(select "Id" from "UserChat" uc3 where uc3."ChatId"=:Chatid order by "Id"asc limit 1) and mi."Time">=uct."At" order by mi."MessageId" ,mi."Time" asc
    ) as latmsg order by timestamp asc;

            """)
            Flux<LoadMessageDTO> load(String Userid,String Chatid);
            //add in error WHEN mt2."Status" IN ('delivered', 'read') THEN true in load
      @Query("""
          SELECT *
FROM (
    SELECT DISTINCT ON (mi."MessageId")
        mi."MessageId" AS msgid,
        uc."ChatId" AS chatid,
        mi."MessageType" AS type,
        CASE 
            WHEN mi."MessageType" = 'text' THEN mi."Message"
            ELSE mi."MediaId"
        END AS content,

        (
  SELECT uc2."UserName"
  FROM "UserChat" uc1
  JOIN "UserChat" uc2
    ON uc2."ChatId" = uc1."ChatId"
   AND uc2."UserId" = mi."SenderId"
  WHERE uc1."Id" = mi."ChatId"
  LIMIT 1
) AS sendername,

  mi."SenderId" as userid,

        mi."Time" AS timestamp,
        mi."RepliedTo" AS repliedto,
        uc_forward."ChatId" AS forwardedfrom,

        (
            SELECT mt2."Status"
            FROM "MessageTrackHistory" mt2
            WHERE mt2."MessageId" = mi."MessageId"
              AND mt2."RecieverId" = :Userid
            ORDER BY 
                CASE mt2."Status"
                    WHEN 'pending' THEN 1
                    WHEN 'delivered' THEN 2
                    WHEN 'read' THEN 3
                    ELSE 4
                END
            LIMIT 1
        ) AS status,

        false AS isold  -- since user is only receiving here

    FROM "MessageInfo" mi
    JOIN "MessageTrackHistory" mt ON mi."MessageId" = mt."MessageId"
    JOIN "UserChat" uc ON uc."Id" = mi."ChatId"
    JOIN "UserInfo" ui ON ui."UserId" = mi."SenderId"
    LEFT JOIN "UserChat" uc_forward ON uc_forward."Id" = mi."ForwardedFrom"

    WHERE mt."RecieverId" = :Userid
    ORDER BY mi."MessageId", mi."Time" ASC
) AS latmsg
WHERE status = 'pending' or status = 'delivered'
ORDER BY timestamp ASC;

          """)
          Flux<LoadMessageDTO> loadpending(String Userid);
     

          @Query("""
              update "MessageInfo" 
              set "DeletedBy"=:userId
              where "MessageId"=:msgId returning *
              """)
              Mono<MessageTrackHistory> deleteforeone(String userId,String msgId);

          @Query("""
              update "MessageInfo" 
              set "DeletedBy"=null,"IsRevived"=true
              where "MessageId"=:msgId and "DeletedBy"=:userId returning *
              """)
              Mono<MessageTrackHistory> reviveforeone(String userId,String msgId);


      @Query("""
          SELECT
    mi."MessageId"        AS msgid,
    uc."ChatId"           AS chatid,
    mi."MessageType"      AS type,
    CASE
        WHEN mi."MessageType" = 'text' THEN mi."Message"
        ELSE mi."MediaId"
    END                   AS content,
    (
  SELECT uc2."UserName"
  FROM "UserChat" uc1
  JOIN "UserChat" uc2
    ON uc2."ChatId" = uc1."ChatId"
   AND uc2."UserId" = mi."SenderId"
  WHERE uc1."Id" = mi."ChatId"
  LIMIT 1
) AS sendername,
    mi."SenderId"           AS userid,
    mi."Time"             AS timestamp,
    mi."RepliedTo"        AS repliedto,
    mi."ForwardedFrom"    AS forwardedfrom,
    mi."DeletedBy"        AS isdeletedone,
    mi."IsRevived"        AS revived,
    mi."SpaceId"          AS spaceid
FROM "MessageInfo" mi
JOIN "UserChat" uc
    ON uc."Id" = mi."ChatId"
JOIN "UserInfo" ui
    ON ui."UserId" = mi."SenderId"
WHERE uc."ChatId" = :Chatid
ORDER BY mi."Time" ASC;

          """)
          Flux<LoadMessageDTO> loadforroom(String Chatid);
          
}
