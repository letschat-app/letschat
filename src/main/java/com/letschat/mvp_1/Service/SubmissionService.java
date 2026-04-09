package com.letschat.mvp_1.Service;

import org.springframework.stereotype.Component;

import com.letschat.mvp_1.Models.SubmissionInfo;
import com.letschat.mvp_1.Repositories.SubmissionRepo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@Component
public class SubmissionService {
    private final SubmissionRepo submissionRepo;
    public SubmissionService(SubmissionRepo submissionRepo){
        this.submissionRepo=submissionRepo;
    }

    public Mono<SubmissionInfo> insert(SubmissionInfo info){
        return submissionRepo.save(info);
    }

    public Flux<SubmissionInfo> getallsubmission(Long id){
        return submissionRepo.findByAssignmentId(id);
    }

    public Mono<SubmissionInfo> getsubmission(Long id ,String userid){
        return submissionRepo.findByAssignmentIdAndUserId(id, userid);
    }
}
