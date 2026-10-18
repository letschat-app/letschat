package com.letschat.mvp_1.DTOs;

import java.time.LocalDateTime;

public class LabsExperimentDTO {

    private Long id;
    private String title;
    private String description;
    private String status;
    private long useCount;
    private long buildCount;
    private long bothCount;
    private String userVotedIntent; // 'use', 'build', 'both', or null
    private boolean isOptedInEarlyAccess;
    private LocalDateTime createdAt;

    public LabsExperimentDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getUseCount() {
        return useCount;
    }

    public void setUseCount(long useCount) {
        this.useCount = useCount;
    }

    public long getBuildCount() {
        return buildCount;
    }

    public void setBuildCount(long buildCount) {
        this.buildCount = buildCount;
    }

    public long getBothCount() {
        return bothCount;
    }

    public void setBothCount(long bothCount) {
        this.bothCount = bothCount;
    }

    public String getUserVotedIntent() {
        return userVotedIntent;
    }

    public void setUserVotedIntent(String userVotedIntent) {
        this.userVotedIntent = userVotedIntent;
    }

    public boolean isOptedInEarlyAccess() {
        return isOptedInEarlyAccess;
    }

    public void setOptedInEarlyAccess(boolean optedInEarlyAccess) {
        isOptedInEarlyAccess = optedInEarlyAccess;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
