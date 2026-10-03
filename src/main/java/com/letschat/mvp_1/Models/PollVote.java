package com.letschat.mvp_1.Models;

import java.time.LocalDateTime;
import org.springframework.data.relational.core.mapping.Table;

@Table("poll_votes")
public class PollVote {
    private Long pollId;
    private Long optionId;
    private String userId;
    private LocalDateTime votedAt;

    public PollVote() {}

    public PollVote(Long pollId, Long optionId, String userId, LocalDateTime votedAt) {
        this.pollId = pollId;
        this.optionId = optionId;
        this.userId = userId;
        this.votedAt = votedAt;
    }

    public Long getPollId() {
        return pollId;
    }

    public void setPollId(Long pollId) {
        this.pollId = pollId;
    }

    public Long getOptionId() {
        return optionId;
    }

    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getVotedAt() {
        return votedAt;
    }

    public void setVotedAt(LocalDateTime votedAt) {
        this.votedAt = votedAt;
    }
}
