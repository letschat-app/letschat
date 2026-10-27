package com.letschat.mvp_1.Repositories;

import java.time.LocalDateTime;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.DTOs.LastMessageDTO;
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
  CASE 
    WHEN mi."SenderId" = :Userid THEN (
      SELECT MAX(mt2."DeliverTime") 
      FROM "MessageTrackHistory" mt2 
      WHERE mt2."MessageId" = mi."MessageId"
    )
    ELSE NULL
  END AS delivertime,

  CASE 
    WHEN mi."SenderId" = :Userid THEN (
      SELECT CASE 
        WHEN COUNT(*) = COUNT(CASE WHEN mt2."Status" = 'read' THEN 1 END) THEN MAX(mt2."ReadTime")
        ELSE NULL
      END
      FROM "MessageTrackHistory" mt2 
      WHERE mt2."MessageId" = mi."MessageId"
    )
    ELSE NULL
  END AS readtime,

  CASE 
    WHEN mi."SenderId" = :Userid THEN (
      SELECT MAX(mt2."MediaOpenedTime") 
      FROM "MessageTrackHistory" mt2 
      WHERE mt2."MessageId" = mi."MessageId"
    )
    ELSE NULL
  END AS mediaopenedtime,

  mi."RepliedTo" as repliedto,
  (select "ChatId" from "UserChat" uc1 where uc1."Id"=mi."ForwardedFrom" limit 1) as forwardedfrom,

  CASE 
    WHEN mi."SenderId" = :Userid THEN (
      SELECT CASE 
        WHEN COUNT(*) = COUNT(CASE WHEN mt2."Status" = 'read' THEN 1 END) THEN 'read'
        WHEN COUNT(CASE WHEN mt2."Status" IN ('delivered', 'read') THEN 1 END) > 0 THEN 'delivered'
        ELSE 'pending'
      END
      FROM "MessageTrackHistory" mt2 
      WHERE mt2."MessageId" = mi."MessageId"
    )
    ELSE NULL
  END AS status,

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
join "UserChat" uct on uct."ChatId"=:Chatid and uct."UserId"=:Userid
JOIN "MessageTrackHistory" mt ON mi."MessageId" = mt."MessageId"
WHERE (mt."SenderId" = :Userid OR mt."RecieverId" = :Userid) and mi."ChatId"=(select "Id" from "UserChat" uc3 where uc3."ChatId"=:Chatid order by "Id"asc limit 1) and (uct."At" IS NULL OR mi."Time">=uct."At") AND COALESCE(mi."SpaceId", 0) = COALESCE(:SpaceId, 0) AND (:before IS NULL OR mi."Time" < :before) order by mi."MessageId" ,mi."Time" desc
    ) as latmsg order by timestamp desc limit 60;

            """)
            Flux<LoadMessageDTO> load(String Userid,String Chatid,Integer SpaceId,LocalDateTime before);
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
        mi."SpaceId" as spaceid,
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

    CASE 
      WHEN mi."SenderId" = :Userid THEN (
        SELECT MAX(mt2."DeliverTime") 
        FROM "MessageTrackHistory" mt2 
        WHERE mt2."MessageId" = mi."MessageId"
      )
      ELSE NULL
    END AS delivertime,

    CASE 
      WHEN mi."SenderId" = :Userid THEN (
        SELECT CASE 
          WHEN COUNT(*) = COUNT(CASE WHEN mt2."Status" = 'read' THEN 1 END) THEN MAX(mt2."ReadTime")
          ELSE NULL
        END
        FROM "MessageTrackHistory" mt2 
        WHERE mt2."MessageId" = mi."MessageId"
      )
      ELSE NULL
    END AS readtime,

    CASE 
      WHEN mi."SenderId" = :Userid THEN (
        SELECT MAX(mt2."MediaOpenedTime") 
        FROM "MessageTrackHistory" mt2 
        WHERE mt2."MessageId" = mi."MessageId"
      )
      ELSE NULL
    END AS mediaopenedtime,

    CASE 
      WHEN mi."SenderId" = :Userid THEN (
        SELECT CASE 
          WHEN COUNT(*) = COUNT(CASE WHEN mt2."Status" = 'read' THEN 1 END) THEN 'read'
          WHEN COUNT(CASE WHEN mt2."Status" IN ('delivered', 'read') THEN 1 END) > 0 THEN 'delivered'
          ELSE 'pending'
        END
        FROM "MessageTrackHistory" mt2 
        WHERE mt2."MessageId" = mi."MessageId"
      )
      ELSE NULL
    END AS status,

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
AND COALESCE(mi."SpaceId", 0) = COALESCE(:spaceId, 0)
AND (:before IS NULL OR mi."Time" < :before)
ORDER BY mi."Time" DESC
Limit 60;

          """)
          Flux<LoadMessageDTO> loadforroom(String Userid,String Chatid,Integer spaceId,LocalDateTime before);

    @Query("""
SELECT * FROM (
    SELECT DISTINCT ON (mi."MessageId")
        mi."MessageId" AS msgid,
        (SELECT "ChatId" FROM "UserChat" uc0 WHERE uc0."Id" = mi."ChatId" LIMIT 1) AS "chatid",
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
        mi."SenderId" AS userid,
        mi."Time" AS timestamp,
        CASE 
            WHEN mi."SenderId" = :Userid THEN (
                SELECT MAX(mt2."DeliverTime") 
                FROM "MessageTrackHistory" mt2 
                WHERE mt2."MessageId" = mi."MessageId"
            )
            ELSE NULL
        END AS delivertime,
        CASE 
            WHEN mi."SenderId" = :Userid THEN (
                SELECT CASE 
                    WHEN COUNT(*) = COUNT(CASE WHEN mt2."Status" = 'read' THEN 1 END) THEN MAX(mt2."ReadTime")
                    ELSE NULL
                END
                FROM "MessageTrackHistory" mt2 
                WHERE mt2."MessageId" = mi."MessageId"
            )
            ELSE NULL
        END AS readtime,
        CASE 
            WHEN mi."SenderId" = :Userid THEN (
                SELECT MAX(mt2."MediaOpenedTime") 
                FROM "MessageTrackHistory" mt2 
                WHERE mt2."MessageId" = mi."MessageId"
            )
            ELSE NULL
        END AS mediaopenedtime,
        mi."RepliedTo" AS repliedto,
        (SELECT "ChatId" FROM "UserChat" uc1 WHERE uc1."Id" = mi."ForwardedFrom" LIMIT 1) AS forwardedfrom,
        CASE 
            WHEN mi."SenderId" = :Userid THEN (
                SELECT CASE 
                    WHEN COUNT(*) = COUNT(CASE WHEN mt2."Status" = 'read' THEN 1 END) THEN 'read'
                    WHEN COUNT(CASE WHEN mt2."Status" IN ('delivered', 'read') THEN 1 END) > 0 THEN 'delivered'
                    ELSE 'pending'
                END
                FROM "MessageTrackHistory" mt2 
                WHERE mt2."MessageId" = mi."MessageId"
            )
            ELSE NULL
        END AS status,
        CASE
            WHEN mi."SenderId" = :Userid THEN true
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
        mi."DeletedBy" AS isdeletedeone,
        mi."IsRevived" AS revived,
        mi."SpaceId" AS spaceid
    FROM "MessageInfo" mi
    JOIN "UserChat" uct ON uct."ChatId" = :Chatid AND uct."UserId" = :Userid
    JOIN "MessageTrackHistory" mt ON mi."MessageId" = mt."MessageId"
    WHERE (mt."SenderId" = :Userid OR mt."RecieverId" = :Userid)
      AND mi."ChatId" = (SELECT "Id" FROM "UserChat" uc3 WHERE uc3."ChatId" = :Chatid ORDER BY "Id" ASC LIMIT 1)
      AND (uct."At" IS NULL OR mi."Time" >= uct."At")
      AND COALESCE(mi."SpaceId", 0) = COALESCE(:SpaceId, 0)
    ORDER BY mi."MessageId", mi."Time" DESC
) AS latmsg ORDER BY timestamp DESC LIMIT :Limit
""")
    Flux<LoadMessageDTO> loadRecent(String Userid, String Chatid, Integer SpaceId, int Limit);

    @Query("""
SELECT
    mi."MessageId"        AS msgid,
    uc."ChatId"           AS chatid,
    COALESCE(
        (SELECT uc2."UserName" FROM "UserChat" uc2 WHERE uc2."ChatId" = uc."ChatId" AND uc2."UserId" = mi."SenderId" LIMIT 1),
        ui."PrivateName",
        ui."PublicName"
    )                     AS sendername,
    mi."SenderId"         AS userid,
    mi."MessageType"      AS type,
    CASE 
        WHEN mi."MessageType" = 'text' THEN mi."Message"
        ELSE mi."MediaId"
    END                   AS content,
    mi."Time"             AS timestamp,
    NULL                  AS delivertime,
    NULL                  AS readtime,
    NULL                  AS mediaopenedtime,
    CASE 
        WHEN mi."SenderId" = :Userid THEN 'read'
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
    END                   AS isold,
    (
        SELECT CASE 
            WHEN mt2."SenderId" = :Userid THEN mt2."DeleteForSender"
            WHEN mt2."RecieverId" = :Userid THEN mt2."DeleteForReceiver"
            ELSE FALSE
        END
        FROM "MessageTrackHistory" mt2 
        WHERE mt2."MessageId" = mi."MessageId"
          AND (:Userid = mt2."SenderId" OR :Userid = mt2."RecieverId")
        LIMIT 1
    )                     AS isdeleted,
    CASE 
        WHEN mi."SenderId" = :Userid THEN (
            SELECT CASE 
                WHEN COUNT(*) = COUNT(CASE WHEN mt2."Status" = 'read' THEN 1 END) THEN 'read'
                WHEN COUNT(CASE WHEN mt2."Status" IN ('delivered', 'read') THEN 1 END) > 0 THEN 'delivered'
                ELSE 'pending'
            END
            FROM "MessageTrackHistory" mt2 
            WHERE mt2."MessageId" = mi."MessageId"
        )
        ELSE NULL
    END                   AS status,
    mi."RepliedTo"        AS repliedto,
    mi."ForwardedFrom"    AS forwardedfrom,
    mi."DeletedBy"        AS isdeletedone,
    mi."IsRevived"        AS revived,
    mi."SpaceId"          AS spaceid
FROM "MessageInfo" mi
JOIN "UserChat" uc ON uc."Id" = mi."ChatId"
JOIN "UserInfo" ui ON ui."UserId" = mi."SenderId"
WHERE uc."ChatId" = :Chatid
  AND COALESCE(mi."SpaceId", 0) = COALESCE(:spaceId, 0)
ORDER BY mi."Time" DESC
LIMIT :Limit
""")
    Flux<LoadMessageDTO> loadRecentForRoom(String Userid, String Chatid, Integer spaceId, int Limit);

    @Query("""
SELECT MAX(mi."Time")
FROM "MessageInfo" mi
JOIN "UserChat" uct ON uct."ChatId" = :Chatid AND uct."UserId" = :Userid
JOIN "MessageTrackHistory" mt ON mi."MessageId" = mt."MessageId"
WHERE (mt."SenderId" = :Userid OR mt."RecieverId" = :Userid)
  AND mi."ChatId" = (SELECT "Id" FROM "UserChat" uc3 WHERE uc3."ChatId" = :Chatid ORDER BY "Id" ASC LIMIT 1)
  AND (uct."At" IS NULL OR mi."Time" >= uct."At")
  AND COALESCE(mi."SpaceId", 0) = COALESCE(:SpaceId, 0)
""")
    Mono<LocalDateTime> findLatestTimestamp(String Userid, String Chatid, Integer SpaceId);

    @Query("""
SELECT MAX(mi."Time")
FROM "MessageInfo" mi
JOIN "UserChat" uc ON uc."Id" = mi."ChatId"
WHERE uc."ChatId" = :Chatid
  AND COALESCE(mi."SpaceId", 0) = COALESCE(:spaceId, 0)
""")
    Mono<LocalDateTime> findLatestTimestampForRoom(String Chatid, Integer spaceId);

    @Query("""
SELECT * FROM (
    SELECT DISTINCT ON (mi."MessageId")
        mi."MessageId" AS msgid,
        (SELECT "ChatId" FROM "UserChat" uc0 WHERE uc0."Id" = mi."ChatId" LIMIT 1) AS chatid,
        mi."MessageType" AS type,
        CASE WHEN mi."MessageType" = 'text' THEN mi."Message" ELSE mi."MediaId" END AS content,
        (SELECT uc2."UserName" FROM "UserChat" uc1 JOIN "UserChat" uc2 ON uc2."ChatId" = uc1."ChatId" AND uc2."UserId" = mi."SenderId" WHERE uc1."Id" = mi."ChatId" LIMIT 1) AS sendername,
        mi."SenderId" AS userid,
        mi."Time" AS timestamp,
        CASE WHEN mi."SenderId" = :Userid THEN (SELECT MAX(mt2."DeliverTime") FROM "MessageTrackHistory" mt2 WHERE mt2."MessageId" = mi."MessageId") ELSE NULL END AS delivertime,
        CASE WHEN mi."SenderId" = :Userid THEN (SELECT CASE WHEN COUNT(*) = COUNT(CASE WHEN mt2."Status" = 'read' THEN 1 END) THEN MAX(mt2."ReadTime") ELSE NULL END FROM "MessageTrackHistory" mt2 WHERE mt2."MessageId" = mi."MessageId") ELSE NULL END AS readtime,
        CASE WHEN mi."SenderId" = :Userid THEN (SELECT MAX(mt2."MediaOpenedTime") FROM "MessageTrackHistory" mt2 WHERE mt2."MessageId" = mi."MessageId") ELSE NULL END AS mediaopenedtime,
        mi."RepliedTo" AS repliedto,
        (SELECT "ChatId" FROM "UserChat" uc1 WHERE uc1."Id" = mi."ForwardedFrom" LIMIT 1) AS forwardedfrom,
        CASE WHEN mi."SenderId" = :Userid THEN (SELECT CASE WHEN COUNT(*) = COUNT(CASE WHEN mt2."Status" = 'read' THEN 1 END) THEN 'read' WHEN COUNT(CASE WHEN mt2."Status" IN ('delivered', 'read') THEN 1 END) > 0 THEN 'delivered' ELSE 'pending' END FROM "MessageTrackHistory" mt2 WHERE mt2."MessageId" = mi."MessageId") ELSE NULL END AS status,
        CASE WHEN mi."SenderId" = :Userid THEN true ELSE (SELECT CASE WHEN mt2."Status" IN ('read') THEN true ELSE false END FROM "MessageTrackHistory" mt2 WHERE mt2."MessageId" = mi."MessageId" AND mt2."RecieverId" = :Userid LIMIT 1) END AS isold,
        (SELECT CASE WHEN mt2."SenderId" = :Userid THEN mt2."DeleteForSender" WHEN mt2."RecieverId" = :Userid THEN mt2."DeleteForReceiver" ELSE FALSE END FROM "MessageTrackHistory" mt2 WHERE mt2."MessageId" = mi."MessageId" AND (:Userid = mt2."SenderId" OR :Userid = mt2."RecieverId") LIMIT 1) AS isdeleted,
        mi."DeletedBy" AS isdeletedeone,
        mi."IsRevived" AS revived,
        mi."SpaceId" AS spaceid
    FROM "MessageInfo" mi
    JOIN "UserChat" uct ON uct."ChatId" = :Chatid AND uct."UserId" = :Userid
    JOIN "MessageTrackHistory" mt ON mi."MessageId" = mt."MessageId"
    WHERE (mt."SenderId" = :Userid OR mt."RecieverId" = :Userid)
      AND mi."ChatId" = (SELECT "Id" FROM "UserChat" uc3 WHERE uc3."ChatId" = :Chatid ORDER BY "Id" ASC LIMIT 1)
      AND (uct."At" IS NULL OR mi."Time" >= uct."At")
      AND COALESCE(mi."SpaceId", 0) = COALESCE(:SpaceId, 0)
      AND (:After IS NULL OR mi."Time" > :After)
    ORDER BY mi."MessageId", mi."Time" ASC
) AS latmsg ORDER BY timestamp ASC LIMIT :Limit
""")
    Flux<LoadMessageDTO> loadPendingAfter(String Userid, String Chatid, Integer SpaceId, LocalDateTime After, int Limit);

    @Query("""
SELECT
    mi."MessageId"        AS msgid,
    uc."ChatId"           AS chatid,
    COALESCE(
        (SELECT uc2."UserName" FROM "UserChat" uc2 WHERE uc2."ChatId" = uc."ChatId" AND uc2."UserId" = mi."SenderId" LIMIT 1),
        ui."PrivateName",
        ui."PublicName"
    )                     AS sendername,
    mi."SenderId"         AS userid,
    mi."MessageType"      AS type,
    CASE 
        WHEN mi."MessageType" = 'text' THEN mi."Message"
        ELSE mi."MediaId"
    END                   AS content,
    mi."Time"             AS timestamp,
    CASE 
        WHEN mi."SenderId" = :Userid THEN (SELECT MAX(mt2."DeliverTime") FROM "MessageTrackHistory" mt2 WHERE mt2."MessageId" = mi."MessageId")
        ELSE NULL
    END                   AS delivertime,
    CASE 
        WHEN mi."SenderId" = :Userid THEN (SELECT MAX(mt2."ReadTime") FROM "MessageTrackHistory" mt2 WHERE mt2."MessageId" = mi."MessageId")
        ELSE NULL
    END                   AS readtime,
    CASE 
        WHEN mi."SenderId" = :Userid THEN (SELECT MAX(mt2."MediaOpenedTime") FROM "MessageTrackHistory" mt2 WHERE mt2."MessageId" = mi."MessageId")
        ELSE NULL
    END                   AS mediaopenedtime,
    CASE
        WHEN mi."SenderId" = :Userid THEN (
            SELECT CASE 
                WHEN COUNT(*) = COUNT(CASE WHEN mt2."Status" = 'read' THEN 1 END) THEN 'read'
                WHEN COUNT(CASE WHEN mt2."Status" IN ('delivered', 'read') THEN 1 END) > 0 THEN 'delivered'
                ELSE 'pending'
            END
            FROM "MessageTrackHistory" mt2 
            WHERE mt2."MessageId" = mi."MessageId"
        )
        ELSE NULL
    END                   AS status,
    mi."RepliedTo"        AS repliedto,
    mi."ForwardedFrom"    AS forwardedfrom,
    mi."DeletedBy"        AS isdeletedeone,
    mi."IsRevived"        AS revived,
    mi."SpaceId"          AS spaceid
FROM "MessageInfo" mi
JOIN "UserChat" uc ON uc."Id" = mi."ChatId"
JOIN "UserInfo" ui ON ui."UserId" = mi."SenderId"
WHERE uc."ChatId" = :Chatid
  AND COALESCE(mi."SpaceId", 0) = COALESCE(:spaceId, 0)
  AND (:After IS NULL OR mi."Time" > :After)
ORDER BY mi."Time" ASC
LIMIT :Limit
""")
    Flux<LoadMessageDTO> loadPendingAfterForRoom(String Chatid, Integer spaceId, LocalDateTime After, int Limit);

    @Query("""
SELECT DISTINCT ON (uc_all."ChatId")
    uc_all."ChatId" AS chatId,
    mi."MessageId" AS messageId,
    mi."MessageType" AS messageType,
    CASE 
        WHEN mi."MessageType" = 'text' THEN mi."Message"
        ELSE mi."MediaId"
    END AS content,
    mi."SenderId" AS senderId,
    COALESCE(
        (SELECT uc_sender."UserName" 
         FROM "UserChat" uc_sender 
         WHERE uc_sender."ChatId" = uc_all."ChatId" 
           AND uc_sender."UserId" = mi."SenderId" 
         LIMIT 1),
        ui."PrivateName", 
        ui."PublicName"
    ) AS senderName,
    mi."Time" AS timestamp
FROM "UserChat" uc_user
JOIN "UserChat" uc_all ON uc_user."ChatId" = uc_all."ChatId"
JOIN "MessageInfo" mi ON mi."ChatId" = (
    SELECT "Id" FROM "UserChat" uc_first 
    WHERE uc_first."ChatId" = uc_user."ChatId" 
    ORDER BY "Id" ASC LIMIT 1
)
LEFT JOIN "UserInfo" ui ON ui."UserId" = mi."SenderId"
WHERE uc_user."UserId" = :userId
  AND (mi."DeletedBy" IS NULL OR mi."DeletedBy" != 'everyone')
  AND (uc_user."At" IS NULL OR mi."Time" >= uc_user."At")
  AND NOT EXISTS (
      SELECT 1 FROM "MessageTrackHistory" mt 
      WHERE mt."MessageId" = mi."MessageId" 
        AND ((mt."SenderId" = :userId AND mt."DeleteForSender" = true) 
          OR (mt."RecieverId" = :userId AND mt."DeleteForReceiver" = true))
  )
ORDER BY uc_all."ChatId", mi."Time" DESC
""")
    Flux<LastMessageDTO> findLastMessageForUserChats(String userId);


              @Query("""
            select count(distinct "SenderId")
            from "MessageInfo"
            """)
    Mono<Long> getTotalUsers();

    @Query("""
            select count(*)
            from (
                select "SenderId"
                from "MessageInfo"
                group by "SenderId"
                having max("SpaceId") = 0
            ) t
            """)
    Mono<Long> getDefaultOnlyUsers();

    @Query("""
            select count(*)
            from (
                select "SenderId"
                from "MessageInfo"
                group by "SenderId"
                having count(distinct "SpaceId") > 1
            ) t
            """)
    Mono<Long> getMultiSpaceUsers();

    @Query("""
            select count(distinct "SenderId")
            from "MessageInfo"
            where "SpaceId" > 0
            """)
    Mono<Long> getCustomSpaceUsers();
          
}
