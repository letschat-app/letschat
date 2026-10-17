package com.letschat.mvp_1.DTOs;

import java.time.Instant;

public class GroupEventCreateDTO {
    private Long eventId;
    private String groupEventId;
    private String chatId;
    private String eventTitle;
    private Instant startTime;
    private Instant endTime;
    private String eventDetails;
    private String eventColor;
    private String msgId;
    private String action = "scheduled";
    private String scope = "ALL";
    private Boolean isGroupVisible = true;
    private String meetingType; // "online" or "offline"
    private String meetingLink;
    private String location;
    private Integer reminderOffset = 60; // 1 hour default
    private String customReminderTime;

    public GroupEventCreateDTO() {}

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getGroupEventId() { return groupEventId; }
    public void setGroupEventId(String groupEventId) { this.groupEventId = groupEventId; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public String getEventDetails() { return eventDetails; }
    public void setEventDetails(String eventDetails) { this.eventDetails = eventDetails; }

    public String getEventColor() { return eventColor; }
    public void setEventColor(String eventColor) { this.eventColor = eventColor; }

    public String getMsgId() { return msgId; }
    public void setMsgId(String msgId) { this.msgId = msgId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getScope() { return scope != null ? scope : "ALL"; }
    public void setScope(String scope) { this.scope = scope; }

    public Boolean getIsGroupVisible() { return isGroupVisible != null ? isGroupVisible : true; }
    public void setIsGroupVisible(Boolean isGroupVisible) { this.isGroupVisible = isGroupVisible; }

    public String getMeetingType() { return meetingType; }
    public void setMeetingType(String meetingType) { this.meetingType = meetingType; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getReminderOffset() { return reminderOffset != null ? reminderOffset : 60; }
    public void setReminderOffset(Integer reminderOffset) { this.reminderOffset = reminderOffset; }

    public String getCustomReminderTime() { return customReminderTime; }
    public void setCustomReminderTime(String customReminderTime) { this.customReminderTime = customReminderTime; }
}
