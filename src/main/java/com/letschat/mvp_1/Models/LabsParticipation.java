package com.letschat.mvp_1.Models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("\"LabsParticipation\"")
public class LabsParticipation {

    @Id
    @Column("id")
    private Long id;

    @Column("experiment_id")
    private Long experimentId;

    @Column("user_id")
    private String userId;

    @Column("intent")
    private String intent; // 'use', 'build', 'both'

    @Column("voted_at")
    private LocalDateTime votedAt;

    public LabsParticipation() {}

    public LabsParticipation(Long experimentId, String userId, String intent) {
        this.experimentId = experimentId;
        this.userId = userId;
        this.intent = intent;
        this.votedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public LocalDateTime getVotedAt() {
        return votedAt;
    }

    public void setVotedAt(LocalDateTime votedAt) {
        this.votedAt = votedAt;
    }
}
