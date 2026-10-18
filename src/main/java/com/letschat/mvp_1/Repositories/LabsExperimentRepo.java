package com.letschat.mvp_1.Repositories;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.letschat.mvp_1.Models.LabsExperiment;

import reactor.core.publisher.Flux;

public interface LabsExperimentRepo extends ReactiveCrudRepository<LabsExperiment, Long> {

    @Query("SELECT * FROM \"LabsExperiment\" WHERE \"status\" != 'concept' ORDER BY \"id\" ASC")
    Flux<LabsExperiment> findAllActive();
}
