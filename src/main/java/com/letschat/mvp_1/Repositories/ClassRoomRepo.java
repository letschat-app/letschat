package com.letschat.mvp_1.Repositories;

import java.time.LocalDateTime;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.DTOs.LoadMessageDTO;
import com.letschat.mvp_1.Models.ClassRoomInfo;

import reactor.core.publisher.Mono;

public interface ClassRoomRepo extends ReactiveCrudRepository<ClassRoomInfo, String>{
    @Query("""
            select * from "ClassRoomInfo" where "room_id"=:id
            """)
            Mono<ClassRoomInfo> findByRoomId(String id);

    @Query("""
            update "ClassRoomInfo" set "no_of_students"=:member where "room_id"=:id
            """)
            Mono<ClassRoomInfo> updateCount(String id,Long member);
    @Query("""
SELECT COUNT(*)
FROM "MessageInfo" mi
JOIN "UserChat" uc
  ON mi."ChatId" = uc."Id"
WHERE uc."ChatId" = :chatid
  AND mi."Time" > :timestamp                        """)
                Mono<Integer> getCountofClassRoomMessages(String chatid,LocalDateTime timestamp);

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
    mi."SenderId"         AS userid,

    mi."Time"             AS timestamp,
    mi."RepliedTo"        AS repliedto,
    uc_forward."ChatId"   AS forwardedfrom,

    false                 AS isold

FROM "MessageInfo" mi

JOIN "UserChat" uc
  ON uc."Id" = mi."ChatId"

JOIN "UserChat" sender_uc
  ON sender_uc."ChatId" = uc."ChatId"
 AND sender_uc."UserId" = mi."SenderId"

JOIN "UserInfo" sender
  ON sender."UserId" = mi."SenderId"

LEFT JOIN "UserChat" uc_forward
  ON uc_forward."Id" = mi."ForwardedFrom"

WHERE uc."ChatId" = :chatId
  AND mi."Time"  > :timestamp

ORDER BY mi."Time" Desc limit 1

                        """)
                        Mono<LoadMessageDTO> checknew(String chatid,LocalDateTime timestamp);
}
