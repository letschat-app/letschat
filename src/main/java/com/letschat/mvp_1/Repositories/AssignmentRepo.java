package com.letschat.mvp_1.Repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.DTOs.MissingAssignmentDTO;
import com.letschat.mvp_1.Models.AssignmentInfo;

import reactor.core.publisher.Flux;

public interface AssignmentRepo extends  ReactiveCrudRepository<AssignmentInfo, Long>{
    @Query("""
            SELECT "uc"."UserId",
       ARRAY_AGG("ai"."assignment_id") AS "missing_assignment_ids"
FROM "UserChat" "uc"
JOIN "AssignmentInfo" "ai"
    ON "uc"."ChatId" = "ai"."chat_id"
LEFT JOIN "SubmissionInfo" "si"
    ON "si"."assignment_id" = "ai"."assignment_id"
    AND "si"."user_id" = "uc"."UserId"
WHERE "si"."submission_id" IS NULL
  AND "ai"."deadline" > NOW()
  AND "ai"."deadline" <= NOW() + INTERVAL '3 days'
GROUP BY "uc"."UserId";
            """)
            Flux<MissingAssignmentDTO> findMissingAssignments();
}
