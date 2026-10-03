package com.letschat.mvp_1.Repositories;

import java.util.Collection;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import com.letschat.mvp_1.Models.PollOption;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PollOptionRepo extends ReactiveCrudRepository<PollOption, Long> {

    @Query("SELECT * FROM poll_options WHERE poll_id = :pollId ORDER BY id ASC")
    Flux<PollOption> findByPollId(Long pollId);

    @Query("SELECT * FROM poll_options WHERE poll_id IN (:pollIds) ORDER BY poll_id ASC, id ASC")
    Flux<PollOption> findByPollIdIn(Collection<Long> pollIds);

    @Query("SELECT COUNT(*) FROM poll_options WHERE poll_id = :pollId")
    Mono<Long> countByPollId(Long pollId);

    @Query("SELECT COUNT(*) > 0 FROM poll_options WHERE poll_id = :pollId AND LOWER(option_text) = LOWER(:optionText)")
    Mono<Boolean> existsByPollIdAndOptionTextIgnoreCase(Long pollId, String optionText);
}
