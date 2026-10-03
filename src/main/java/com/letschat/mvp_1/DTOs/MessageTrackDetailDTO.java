package com.letschat.mvp_1.DTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class MessageTrackDetailDTO {
    private String receiverId;
    private String receiverName;
    private String receiverProfile;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deliverTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime readTime;

    public MessageTrackDetailDTO() {}

    public MessageTrackDetailDTO(String receiverId, String receiverName, String receiverProfile, String status, LocalDateTime deliverTime, LocalDateTime readTime) {
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.receiverProfile = receiverProfile;
        this.status = status;
        this.deliverTime = deliverTime;
        this.readTime = readTime;
    }

    public String getReceiverId() {
        return receiverId;
    }
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverName() {
        return receiverName;
    }
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverProfile() {
        return receiverProfile;
    }
    public void setReceiverProfile(String receiverProfile) {
        this.receiverProfile = receiverProfile;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDeliverTime() {
        return deliverTime;
    }
    public void setDeliverTime(LocalDateTime deliverTime) {
        this.deliverTime = deliverTime;
    }

    public LocalDateTime getReadTime() {
        return readTime;
    }
    public void setReadTime(LocalDateTime readTime) {
        this.readTime = readTime;
    }
}
