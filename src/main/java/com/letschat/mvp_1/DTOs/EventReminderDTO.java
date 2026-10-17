package com.letschat.mvp_1.DTOs;

public class EventReminderDTO {
    private Long eventId;
    private Boolean reminderEnabled;
    private Integer reminderOffset; // minutes
    private String customReminderTime;

    public EventReminderDTO() {}

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Boolean getReminderEnabled() { return reminderEnabled != null ? reminderEnabled : true; }
    public void setReminderEnabled(Boolean reminderEnabled) { this.reminderEnabled = reminderEnabled; }

    public Integer getReminderOffset() { return reminderOffset != null ? reminderOffset : 60; }
    public void setReminderOffset(Integer reminderOffset) { this.reminderOffset = reminderOffset; }

    public String getCustomReminderTime() { return customReminderTime; }
    public void setCustomReminderTime(String customReminderTime) { this.customReminderTime = customReminderTime; }
}
