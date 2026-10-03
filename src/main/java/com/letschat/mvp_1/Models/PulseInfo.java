package com.letschat.mvp_1.Models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("PulseInfo")
public class PulseInfo {
    @Id
    @Column("pulse_id")
    private Long pulseId;

    @Column("user_id")
    private String userId;

    @Column("type")
    private String type; // "text", "image", "video", "audio", "image_audio"

    @Column("content")
    private String content;

    @Column("pulse_media_id")
    private Long pulseMediaId;

    @Column("bg_color")
    private String bgColor;

    @Column("font_style")
    private String fontStyle;

    @Column("visibility")
    private String visibility; // "chats_only", "everyone"

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("expires_at")
    private LocalDateTime expiresAt;

    @Column("status")
    private String status; // "active", "expired"

    public PulseInfo() {}

    public Long getPulseId() {
        return pulseId;
    }
    public void setPulseId(Long pulseId) {
        this.pulseId = pulseId;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
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

    public Long getPulseMediaId() {
        return pulseMediaId;
    }
    public void setPulseMediaId(Long pulseMediaId) {
        this.pulseMediaId = pulseMediaId;
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
}
