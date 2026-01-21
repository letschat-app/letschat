package com.letschat.mvp_1.Repositories;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.AssignmentInfo;

public interface AssignmentRepo extends  ReactiveCrudRepository<AssignmentInfo, Long>{
    
}
