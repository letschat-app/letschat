package com.letschat.mvp_1.DTOs;
//package com.letschat.mvp_1.DTOs;

import java.time.LocalDateTime;

public class SendingMessageDTO {
    private String msgid;
    private String chatid;
    private String sendername;
    private String type;
    private String content;
    private LocalDateTime timestamp;
    private String repliedto;
    private String forwardedfrom;
    private Integer spaceid;

    public SendingMessageDTO(){}
    public SendingMessageDTO(Integer spaceid,String msgid,String chatid,String sendername,String type,String content,LocalDateTime timestamp,String repliedto,String forwardedfrom){
        this.msgid=msgid;
        this.chatid=chatid;
        this.sendername=sendername;
        this.type=type;
        this.content=content;
        this.timestamp=timestamp;
        this.repliedto=repliedto;
        this.forwardedfrom=forwardedfrom;
        this.spaceid=spaceid;
    }

    public String getmsgid(){
        return msgid;
    }
    public void setmsgid(String msgid){
        this.msgid=msgid;
    }

    public String getchatid(){
        return chatid;
    }
    public void setchatid(String chatid){
        this.chatid=chatid;
    }

    public String getsendername(){
        return sendername;
    }
    public void setsendername(String sendername){
        this.sendername=sendername;
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

