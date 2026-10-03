package com.letschat.mvp_1.Repositories;

import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import com.letschat.mvp_1.Models.Poll;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PollRepo extends ReactiveCrudRepository<Poll, Long> {

    @Query("SELECT * FROM polls WHERE msg_id = :msgId LIMIT 1")
    Mono<Poll> findByMsgId(String msgId);

    @Query("SELECT * FROM polls WHERE msg_id IN (:msgIds)")
    Flux<Poll> findByMsgIdIn(Collection<String> msgIds);

    @Modifying
    @Query("UPDATE polls SET msg_id = :msgId WHERE id = :pollId AND created_by = :sender AND chat_id = :chatId AND (msg_id IS NULL OR msg_id = :msgId)")
    Mono<Integer> linkPollToMessage(Long pollId, String msgId, String sender, String chatId);

    @Modifying
    @Query("DELETE FROM polls WHERE msg_id IS NULL AND created_at < :cutoff")
    Mono<Integer> deleteUnlinkedPolls(LocalDateTime cutoff);
}
