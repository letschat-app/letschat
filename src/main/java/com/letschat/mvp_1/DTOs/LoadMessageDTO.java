package com.letschat.mvp_1.DTOs;
//package com.letschat.mvp_1.DTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class LoadMessageDTO {
    private String msgid;
    private String chatid;
    private String sendername;
    private String userid;
    private String type;
    private String content;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private String repliedto;
    private String forwardedfrom;
    private String status;
    private Boolean isold;
    private Boolean isdeleted;
    private String isdeletedeone;
    private Boolean revived;
    private Integer spaceid;

    public LoadMessageDTO(){}
    public LoadMessageDTO(String msgid,String chatid,String sendername,String userid,String type,String content,LocalDateTime timestamp,String repliedto,String forwardedfrom,String status,Boolean isold,Boolean isdeleted,String isdeletedeone,Boolean revived,Integer spaceid){
        this.msgid=msgid;
        this.chatid=chatid;
        this.sendername=sendername;
        this.type=type;
        this.content=content;
        this.timestamp=timestamp;
        this.repliedto=repliedto;
        this.forwardedfrom=forwardedfrom;
        this.status=status;
        this.isold=isold;
        this.isdeleted=isdeleted;
        this.isdeletedeone=isdeletedeone;
        this.revived=revived;
        this.spaceid=spaceid;
        this.userid=userid;
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

    public String getuserid(){
        return userid;
    }
    public void setuserid(String userid){
        this.userid=userid;
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

    public String getstatus(){
        return status;
    }
    public void setstatus(String status){
        this.status=status;
    }

    public Boolean getisold(){
        return isold;
    }
    public void setisold(Boolean isold){
        this.isold=isold;
    }

    public Boolean getisdeleted(){
        return isdeleted;
    }
    public void setisdeleted(Boolean isdeleted){
        this.isdeleted=isdeleted;
    }

    public String getisdeletedeone(){
        return isdeletedeone;
    }
    public void setisdeletedeone(String isdel){
        this.isdeletedeone=isdel;
    }

    public Boolean getrevived(){
        return revived;
    }
    public void setrevived(Boolean rev){
        this.revived=rev;
    }

    public Integer getspaceid(){
        return spaceid;
    }
    public void setspaceid(Integer spaceid){
        this.spaceid=spaceid;
    }
}

