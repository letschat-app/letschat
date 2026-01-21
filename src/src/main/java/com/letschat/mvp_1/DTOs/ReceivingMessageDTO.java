package com.letschat.mvp_1.DTOs;

import java.time.LocalDateTime;

public class ReceivingMessageDTO {
    private String tempmsgid;
    private String chatid;
    private String type;
    private String content;
    private LocalDateTime timestamp;
    private String repliedto;
    private String forwardedfrom;
    private Integer spaceid;

    public ReceivingMessageDTO(){}
    public ReceivingMessageDTO(Integer spaceid,String tempmsgid,String chatid,String type,String content,LocalDateTime timestamp,String repliedto,String forwardedfrom){
        this.tempmsgid=tempmsgid;
        this.chatid=chatid;
        this.type=type;
        this.content=content;
        this.timestamp=timestamp;
        this.repliedto=repliedto;
        this.forwardedfrom=forwardedfrom;
        this.spaceid=spaceid;
    }

    public String gettempmsgid(){
        return tempmsgid;
    }
    public void settempmsgid(String tempmsgid){
        this.tempmsgid=tempmsgid;
    }

    public String getchatid(){
        return chatid;
    }
    public void setchatid(String chatid){
        this.chatid=chatid;
    }

    public String gettype(){
        return type;
    }
    public void settype(String type){
        this.type=type;
    }

    public String getcontent(){
        return content;
    }
    public void setcontent(String content){
        this.content=content;
    }

    public LocalDateTime gettimestamp(){
        return timestamp;
    }
    public void settimestamp(LocalDateTime timestamp){
        this.timestamp=timestamp;
    }

    public String getrepliedto(){
        return repliedto;
    }
    public void setrepliedto(String repliedto){
        this.repliedto=repliedto;
    }

    public String getforwardedfrom(){
        return forwardedfrom;
    }
    public void setforwardedfrom(String forwardedfrom){
        this.forwardedfrom=forwardedfrom;
    }

    public Integer getspaceid(){
        return spaceid;
    }
    public void setspaceid(Integer spaceid){
        this.spaceid=spaceid;
    }
}
