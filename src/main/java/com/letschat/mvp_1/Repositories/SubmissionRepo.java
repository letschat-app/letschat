package com.letschat.mvp_1.Repositories;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.SubmissionInfo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface SubmissionRepo extends ReactiveCrudRepository<SubmissionInfo,Long>{
    Flux<SubmissionInfo> findByAssignmentId(long assignmentId);

    Mono<SubmissionInfo> findByAssignmentIdAndUserId(long assignmentId, String userId);
}
