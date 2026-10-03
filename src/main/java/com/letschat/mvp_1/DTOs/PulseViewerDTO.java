package com.letschat.mvp_1.DTOs;

import java.time.LocalDateTime;

public class PulseViewerDTO {
    private String viewerUserId;
    private String viewerName;
    private String viewerProfile;
    private LocalDateTime viewedAt;

    public PulseViewerDTO() {}

    public PulseViewerDTO(String viewerUserId, String viewerName, String viewerProfile, LocalDateTime viewedAt) {
        this.viewerUserId = viewerUserId;
        this.viewerName = viewerName;
        this.viewerProfile = viewerProfile;
        this.viewedAt = viewedAt;
    }

    public String getViewerUserId() {
        return viewerUserId;
    }
    public void setViewerUserId(String viewerUserId) {
        this.viewerUserId = viewerUserId;
    }

    public String getViewerName() {
        return viewerName;
    }
    public void setViewerName(String viewerName) {
        this.viewerName = viewerName;
    }

    public String getViewerProfile() {
        return viewerProfile;
    }
    public void setViewerProfile(String viewerProfile) {
        this.viewerProfile = viewerProfile;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }
    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
}
