package com.letschat.mvp_1.Models;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

@Table("\"EventInfo\"")
public class EventInfo {
    @Id
    private Long eventId;
    private String userId;
    private String eventTitle;
    private Instant startTime;
    private Instant endTime;
    private String eventDetails;
    private String eventColor;
    private String msgId;
    private String chatId;
    private String action;
    private Boolean isAdded;
    private Boolean isSync;

    // Enhanced Group & Meeting attributes
    private String groupEventId;
    private String creatorId;
    private String scope = "ALL";
    private Boolean isGroupVisible = true;
    private String meetingType; // "online" or "offline"
    private String meetingLink;
    private String location;
    private Boolean reminderEnabled = true;
    private Integer reminderOffset = 60; // default 1 hour before
    private String customReminderTime;
    private String rsvpStatus; // "going", "not_going", "maybe"
    private String externalBookerName;
    private String externalBookerEmail;
    private String externalStatus; // "pending", "approved", "declined", "proposed_by_host", "proposed_by_guest"
    private String bookingToken;
    private String guestUserId;
    private String lastModifiedBy; // "HOST" or "GUEST"
    private String changeMessage;
    @Transient
    private String creatorName;

    public EventInfo(){}

    public EventInfo(Long eventId, String userId, String eventTitle, Instant startTime, Instant endTime, 
                     String eventDetails, String eventColor, String msgId, String chatId, String action, 
                     Boolean isAdded, Boolean isSync){
        this.eventId = eventId;
        this.userId = userId;
        this.eventTitle = eventTitle;
        this.startTime = startTime;
        this.endTime = endTime;
        this.eventDetails = eventDetails;
        this.eventColor = eventColor;
        this.msgId = msgId;
        this.chatId = chatId;
        this.action = action;
        this.isAdded = isAdded;
        this.isSync = isSync;
    }

    public Long getEventId() {
        return eventId;
    }
    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventTitle() {
        return eventTitle;
    }
    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public Instant getStartTime() {
        return startTime;
    }
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public String getEventDetails() {
        return eventDetails;
    }
    public void setEventDetails(String eventDetails) {
        this.eventDetails = eventDetails;
    }

    public String getEventColor() {
        return eventColor;
    }
    public void setEventColor(String eventColor) {
        this.eventColor = eventColor;
    }

    public String getMsgId() {
        return msgId;
    }
    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getChatId() {
        return chatId;
    }
    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getAction(){
        return action;
    }
    public void setAction(String action){
        this.action = action;
    }

    public Boolean getIsAdded(){
        return isAdded;
    }
    public void setIsAdded(Boolean isAdded){
        this.isAdded = isAdded;
    }

    public Boolean getIsSync(){
        return isSync;
    }
    public void setIsSync(Boolean isSync){
        this.isSync = isSync;
    }

    public String getGroupEventId() {
        return groupEventId;
    }
    public void setGroupEventId(String groupEventId) {
        this.groupEventId = groupEventId;
    }

    public String getCreatorId() {
        return creatorId;
    }
    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getScope() {
        return scope;
    }
    public void setScope(String scope) {
        this.scope = scope;
    }

    public Boolean getIsGroupVisible() {
        return isGroupVisible != null ? isGroupVisible : true;
    }
    public void setIsGroupVisible(Boolean isGroupVisible) {
        this.isGroupVisible = isGroupVisible;
    }

    public String getMeetingType() {
        return meetingType;
    }
    public void setMeetingType(String meetingType) {
        this.meetingType = meetingType;
    }

    public String getMeetingLink() {
        return meetingLink;
    }
    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }

    public Boolean getReminderEnabled() {
        return reminderEnabled != null ? reminderEnabled : true;
    }
    public void setReminderEnabled(Boolean reminderEnabled) {
        this.reminderEnabled = reminderEnabled;
    }

    public Integer getReminderOffset() {
        return reminderOffset != null ? reminderOffset : 60;
    }
    public void setReminderOffset(Integer reminderOffset) {
        this.reminderOffset = reminderOffset;
    }

    public String getCustomReminderTime() {
        return customReminderTime;
    }
    public void setCustomReminderTime(String customReminderTime) {
        this.customReminderTime = customReminderTime;
    }

    public String getRsvpStatus() {
        return rsvpStatus;
    }
    public void setRsvpStatus(String rsvpStatus) {
        this.rsvpStatus = rsvpStatus;
    }

    public String getExternalBookerName() {
        return externalBookerName;
    }
    public void setExternalBookerName(String externalBookerName) {
        this.externalBookerName = externalBookerName;
    }

    public String getExternalBookerEmail() {
        return externalBookerEmail;
    }
    public void setExternalBookerEmail(String externalBookerEmail) {
        this.externalBookerEmail = externalBookerEmail;
    }

    public String getExternalStatus() {
        return externalStatus;
    }
    public void setExternalStatus(String externalStatus) {
        this.externalStatus = externalStatus;
    }

    public String getBookingToken() {
        return bookingToken;
    }
    public void setBookingToken(String bookingToken) {
        this.bookingToken = bookingToken;
    }

    public String getGuestUserId() {
        return guestUserId;
    }
    public void setGuestUserId(String guestUserId) {
        this.guestUserId = guestUserId;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }
    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public String getChangeMessage() {
        return changeMessage;
    }
    public void setChangeMessage(String changeMessage) {
        this.changeMessage = changeMessage;
    }

    public String getCreatorName() {
        return creatorName;
    }
    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }
}
