package com.letschat.mvp_1.Models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
@Table("MessageInfo")
public class MessageInfo {
    @Id
    private String MessageId;
    private Long ChatId;
    private String SenderId;
    private String MessageType;
    private String Message;
    private String MediaId;
    private String RepliedTo;
    private Long ForwardedFrom;
    private LocalDateTime Time;
    private Integer SpaceId;

    public MessageInfo(){}
    public MessageInfo(String MessageId,Long ChatId,String SenderId,String MessageType,String Message,String MediaId
        ,String RepliedTo,Long ForwardedFrom,LocalDateTime Time,Integer SpaceId){
        this.MessageId=MessageId;
        this.ChatId=ChatId;
        this.SenderId=SenderId;
        this.MessageType=MessageType;
        this.Message=Message;
        this.MediaId=MediaId;
        this.RepliedTo=RepliedTo;
        this.ForwardedFrom=ForwardedFrom;
        this.Time=Time;
        this.SpaceId=SpaceId;
    }

    public String getMessageId(){
        return MessageId;
    }
    public void setMessageId(String MessageId){
        this.MessageId=MessageId;
    }

    public Long getChatId(){
        return ChatId;
    }
    public void setChatId(Long ChatId){
        this.ChatId=ChatId;
    }

    public String getSenderId(){
        return SenderId;
    }
    public void setSenderId(String SenderId){
        this.SenderId=SenderId;
    }

    public String getMessageType(){
        return MessageType;
    }
    public void setMessageType(String MessageType){
        this.MessageType=MessageType;
    }

    public String getMessage(){
        return Message;
    }
    public void setMessage(String Message){
        this.Message=Message;
    }

    public String getMediaId(){
        return MediaId;
    }
    public void setMediaId(String MediaId){
        this.MediaId=MediaId;
    }

    public String getRepliedTo(){
        return RepliedTo;
    }
    public void setRepliedTo(String RepliedTo){
        this.RepliedTo=RepliedTo;
    }

    public Long getForwardedFrom(){
        return ForwardedFrom;
    }
    public void setForwardedFrom(Long ForwardedFrom){
        this.ForwardedFrom=ForwardedFrom;
    }

    public LocalDateTime getTime(){
        return Time;
    }
    public void setTime(LocalDateTime Time){
        this.Time=Time;
    }

    public Integer getSpaceId(){
        return SpaceId;
    }
    public void setSpaceId(Integer spaceid){
        this.SpaceId=spaceid;
    }
}
