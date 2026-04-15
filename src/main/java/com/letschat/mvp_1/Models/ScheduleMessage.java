package com.letschat.mvp_1.Models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("\"ScheduleMessageInfo\"")
public class ScheduleMessage {
    @Id
    private Long msgId;
    private String chatId;
    private String senderId;
    private String messageType;
    private String message;
    private String mediaId;
    private Integer spaceId;
    private LocalDateTime time;

    public ScheduleMessage(){}
    public ScheduleMessage(Long msgId,String chatId,String senderId,String messageType,String message,String mediaId,Integer spaceId,LocalDateTime time){
        this.msgId=msgId;
        this.chatId=chatId;
        this.senderId=senderId;
        this.messageType=messageType;
        this.message=message;
        this.mediaId=mediaId;
        this.spaceId=spaceId;
        this.time=time;
    }

    public void setMessageId(Long msgId){
        this.msgId=msgId;
    }
    public Long getMessageId(){
        return msgId;
    }

    public void setChatId(String chatid){
        this.chatId=chatid;
    }
    public String getChatId(){
        return chatId;
    }

    public void setSenderId(String sender){
        this.senderId=sender;
    }
    public String getSenderId(){
        return senderId;
    }

    public void setMessageType(String type){
        this.messageType=type;
    }
    public String getMessageType(){
        return messageType;
    }

    public void setMessage(String msg){
        this.message=msg;
    }
    public String getMessage(){
        return message;
    }

    public void setMediaId(String media){
        this.mediaId=media;
    }
    public String getMediaId(){
        return mediaId;
    }

    public void setSpaceId(Integer space){
        this.spaceId=space;
    }
    public Integer getSpaceId(){
        return spaceId;
    }

    public LocalDateTime getTime(){
        return time;
    }
    public void setTime(LocalDateTime time){
        this.time=time;
    }
}
