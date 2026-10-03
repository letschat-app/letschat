package com.letschat.mvp_1.Repositories;

import java.util.Collection;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import com.letschat.mvp_1.Models.PollVote;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PollVoteRepo extends ReactiveCrudRepository<PollVote, Long> {

    @Modifying
    @Query("DELETE FROM poll_votes WHERE poll_id = :pollId AND user_id = :userId")
    Mono<Integer> deleteByPollIdAndUserId(Long pollId, String userId);

    @Query("SELECT poll_id, option_id, user_id FROM poll_votes WHERE poll_id = :pollId")
    Flux<VoteRecord> findVotesByPollId(Long pollId);

    @Query("SELECT poll_id, option_id, user_id FROM poll_votes WHERE poll_id IN (:pollIds)")
    Flux<VoteRecord> findVotesByPollIdIn(Collection<Long> pollIds);

    @Query("SELECT option_id FROM poll_votes WHERE poll_id = :pollId AND user_id = :userId")
    Flux<Long> findUserVotes(Long pollId, String userId);

    @Query("SELECT poll_id, option_id FROM poll_votes WHERE poll_id IN (:pollIds) AND user_id = :userId")
    Flux<UserVoteRecord> findUserVotesByPollIdsAndUserId(Collection<Long> pollIds, String userId);

    record VoteRecord(Long pollId, Long optionId, String userId) {}
    record UserVoteRecord(Long pollId, Long optionId) {}
}
