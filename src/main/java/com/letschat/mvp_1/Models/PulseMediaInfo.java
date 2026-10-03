package com.letschat.mvp_1.Models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("PulseMediaInfo")
public class PulseMediaInfo {
    @Id
    @Column("pulse_media_id")
    private Long pulseMediaId;

    @Column("image_file_key")
    private String imageFileKey;

    @Column("audio_file_key")
    private String audioFileKey;

    @Column("video_file_key")
    private String videoFileKey;

    @Column("created_at")
    private LocalDateTime createdAt;

    public PulseMediaInfo() {}

    public Long getPulseMediaId() {
        return pulseMediaId;
    }
    public void setPulseMediaId(Long pulseMediaId) {
        this.pulseMediaId = pulseMediaId;
    }

    public String getImageFileKey() {
        return imageFileKey;
    }
    public void setImageFileKey(String imageFileKey) {
        this.imageFileKey = imageFileKey;
    }

    public String getAudioFileKey() {
        return audioFileKey;
    }
    public void setAudioFileKey(String audioFileKey) {
        this.audioFileKey = audioFileKey;
    }

    public String getVideoFileKey() {
        return videoFileKey;
    }
    public void setVideoFileKey(String videoFileKey) {
        this.videoFileKey = videoFileKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
