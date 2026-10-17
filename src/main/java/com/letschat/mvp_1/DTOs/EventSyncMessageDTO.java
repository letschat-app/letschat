package com.letschat.mvp_1.DTOs;

import java.time.Instant;
import java.util.Map;

public class EventSyncMessageDTO {
    private String type; // "event_sync", "event_rsvp_update", "booking_request"
    private String groupEventId;
    private String chatId;
    private String msgId;
    private String eventTitle;
    private Instant startTime;
    private Instant endTime;
    private String action;
    private String eventColor;
    private String meetingType;
    private String meetingLink;
    private String location;
    private String updatedBy;
    private Map<String, Long> rsvpCounts;

    public EventSyncMessageDTO() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getGroupEventId() { return groupEventId; }
    public void setGroupEventId(String groupEventId) { this.groupEventId = groupEventId; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getMsgId() { return msgId; }
    public void setMsgId(String msgId) { this.msgId = msgId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEventColor() { return eventColor; }
    public void setEventColor(String eventColor) { this.eventColor = eventColor; }

    public String getMeetingType() { return meetingType; }
    public void setMeetingType(String meetingType) { this.meetingType = meetingType; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public Map<String, Long> getRsvpCounts() { return rsvpCounts; }
    public void setRsvpCounts(Map<String, Long> rsvpCounts) { this.rsvpCounts = rsvpCounts; }
}
