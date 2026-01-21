package com.letschat.mvp_1.Models;

import java.time.Instant;

import org.springframework.data.annotation.Id;
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

    public EventInfo(){}
    public EventInfo(Long eventId, String userId, String eventTitle,Instant startTime, Instant endTime, String eventDetails,String eventColor, String msgId, String chatId, String action, Boolean isAdded, Boolean isSync){
        this.eventId = eventId;
        this.userId = userId;
        this.eventTitle = eventTitle;
        this.startTime = startTime;
        this.endTime = endTime;
        this.eventDetails = eventDetails;
        this.eventColor = eventColor;
        this.msgId = msgId;
        this.chatId = chatId;
        this.action=action;
        this.isAdded=isAdded;
        this.isSync=isSync;
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
        this.action=action;
    }

    public Boolean getIsAdded(){
        return isAdded;
    }
    public void setIsAdded(Boolean isAdded){
        this.isAdded=isAdded;
    }

    public Boolean getIsSync(){
        return isSync;
    }
    public void setIsSync(Boolean isSync){
        this.isSync=isSync;
    }
}
