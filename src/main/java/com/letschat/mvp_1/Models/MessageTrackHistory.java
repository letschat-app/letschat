package com.letschat.mvp_1.Models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
@Table("MessageTrackHistory")
public class MessageTrackHistory {
    @Id
    private Long Id;
    @Column("MessageId")
    private String MessageId;
    @Column("SenderId")
    private String SenderId;
    @Column("RecieverId")
    private String RecieverId;
    private String Status;
    private LocalDateTime DeliverTime;
    private LocalDateTime ReadTime;
    private Boolean DeleteForSender;
    private Boolean DeleteForReceiver;

    public MessageTrackHistory(){}
    public MessageTrackHistory(Long Id,String MessageId,String SenderId,String RecieverId,String Status,
    LocalDateTime DeliverTime,LocalDateTime ReadTime,Boolean DeleteForSender,Boolean DeleteForReceiver){
        this.Id=Id;
        this.MessageId=MessageId;
        this.SenderId=SenderId;
        this.RecieverId=RecieverId;
        this.Status=Status;
        this.DeliverTime=DeliverTime;
        this.ReadTime=ReadTime;
        this.DeleteForSender=DeleteForSender;
        this.DeleteForReceiver=DeleteForReceiver;
    }

    public Long getId(){
        return Id;
    }
    public void setId(Long Id){
        this.Id=Id;
    }

    public String getMessageId(){
        return MessageId;
    }
    public void setMessageId(String MessageId){
        this.MessageId=MessageId;
    }

    public String getSenderId(){
        return SenderId;
    }
    public void setSenderId(String SenderId){
        this.SenderId=SenderId;
    }

    public String getRecieverId(){
        return RecieverId;
    }
    public void setRecieverId(String RecieverId){
        this.RecieverId=RecieverId;
    }

    public String getStatus(){
        return Status;
    }
    public void setStatus(String Status){
        this.Status=Status;
    }

    public LocalDateTime getDeliverTime(){
        return DeliverTime;
    }
    public void setDeliverTime(LocalDateTime DeliverTime){
        this.DeliverTime=DeliverTime;
    }

    public LocalDateTime getReadTime(){
        return ReadTime;
    }
    public void setReadTime(LocalDateTime ReadTime){
        this.ReadTime=ReadTime;
    }

    public Boolean getDeleteForSender(){
        return DeleteForSender;
    }
    public void setDeleteForSender(Boolean DeleteForSender){
        this.DeleteForSender=DeleteForSender;
    }

    public Boolean getDeleteForReceiver(){
        return DeleteForReceiver;
    }
    public void setDeleteForReceiver(Boolean DeleteForReceiver){
        this.DeleteForReceiver=DeleteForReceiver;
    }

}
