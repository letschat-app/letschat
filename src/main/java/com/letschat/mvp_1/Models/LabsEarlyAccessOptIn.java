package com.letschat.mvp_1.Models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("\"LabsEarlyAccessOptIn\"")
public class LabsEarlyAccessOptIn {

    @Id
    @Column("id")
    private Long id;

    @Column("experiment_id")
    private Long experimentId;

    @Column("user_id")
    private String userId;

    @Column("opted_in_at")
    private LocalDateTime optedInAt;

    public LabsEarlyAccessOptIn() {}

    public LabsEarlyAccessOptIn(Long experimentId, String userId) {
        this.experimentId = experimentId;
        this.userId = userId;
        this.optedInAt = LocalDateTime.now();
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

    public LocalDateTime getOptedInAt() {
        return optedInAt;
    }

    public void setOptedInAt(LocalDateTime optedInAt) {
        this.optedInAt = optedInAt;
    }
}
