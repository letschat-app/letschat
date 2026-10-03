package com.letschat.mvp_1.DTOs;

import java.time.LocalDateTime;
import java.util.List;

public class MyPulseDetailDTO {
    private Long pulseId;
    private String type;
    private String content;
    private String bgColor;
    private String fontStyle;
    private String visibility;

    private String imageUrl;
    private String audioUrl;
    private String videoUrl;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String status; // "active", "expired"

    private int totalViews;
    private List<PulseViewerDTO> viewers;

    public MyPulseDetailDTO() {}

    public Long getPulseId() {
        return pulseId;
    }
    public void setPulseId(Long pulseId) {
        this.pulseId = pulseId;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getBgColor() {
        return bgColor;
    }
    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    public String getFontStyle() {
        return fontStyle;
    }
    public void setFontStyle(String fontStyle) {
        this.fontStyle = fontStyle;
    }

    public String getVisibility() {
        return visibility;
    }
    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }
    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotalViews() {
        return totalViews;
    }
    public void setTotalViews(int totalViews) {
        this.totalViews = totalViews;
    }

    public List<PulseViewerDTO> getViewers() {
        return viewers;
    }
    public void setViewers(List<PulseViewerDTO> viewers) {
        this.viewers = viewers;
    }
}
