package com.letschat.mvp_1.DTOs;

public class EventRsvpDTO {
    private String groupEventId;
    private String rsvpStatus; // "going", "cant_join", "maybe"
    private String chatId;

    public EventRsvpDTO() {}

    public EventRsvpDTO(String groupEventId, String rsvpStatus, String chatId) {
        this.groupEventId = groupEventId;
        this.rsvpStatus = rsvpStatus;
        this.chatId = chatId;
    }

    public String getGroupEventId() { return groupEventId; }
    public void setGroupEventId(String groupEventId) { this.groupEventId = groupEventId; }

    public String getRsvpStatus() { return rsvpStatus; }
    public void setRsvpStatus(String rsvpStatus) { this.rsvpStatus = rsvpStatus; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
}
