package com.letschat.mvp_1.Service;


import org.springframework.stereotype.Component;

import com.letschat.mvp_1.Models.AssignmentInfo;
import com.letschat.mvp_1.Repositories.AssignmentRepo;

import reactor.core.publisher.Mono;
@Component
public class AssignmentService {
    private final AssignmentRepo assignmentRepo;
    public AssignmentService(AssignmentRepo assignmentRepo){
        this.assignmentRepo=assignmentRepo;
    }
    public Mono<Long> post(AssignmentInfo assignment){
        if (assignment.getCreated() == null) {
            assignment.setCreated(java.time.Instant.now());
        }
        return assignmentRepo.save(assignment)
        .map(AssignmentInfo::getAssignmentId);
    }

    public Mono<AssignmentInfo> get(Long id){
        return assignmentRepo.findById(id)
        .flatMap(assignment->Mono.just(assignment));
    }
}
