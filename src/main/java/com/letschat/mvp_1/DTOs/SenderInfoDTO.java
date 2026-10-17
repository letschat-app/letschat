package com.letschat.mvp_1.DTOs;

public class SenderInfoDTO {
    private String senderId;
    private String senderName;

    public SenderInfoDTO() {}

    public SenderInfoDTO(String senderId, String senderName) {
        this.senderId = senderId;
        this.senderName = senderName;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
}
