package com.letschat.mvp_1.Models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("PulseViewHistory")
public class PulseViewHistory {
    @Id
    @Column("id")
    private Long id;

    @Column("pulse_id")
    private Long pulseId;

    @Column("viewer_user_id")
    private String viewerUserId;

    @Column("viewed_at")
    private LocalDateTime viewedAt;

    public PulseViewHistory() {}

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Long getPulseId() {
        return pulseId;
    }
    public void setPulseId(Long pulseId) {
        this.pulseId = pulseId;
    }

    public String getViewerUserId() {
        return viewerUserId;
    }
    public void setViewerUserId(String viewerUserId) {
        this.viewerUserId = viewerUserId;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }
    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
}
