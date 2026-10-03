package com.letschat.mvp_1.DTOs;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ACKMessageDTO {
    private String chatid;
    private String tempmsgid;
    private String msgid;
    private List<String> msgids;
    private String status;

    private List<String> pending_users;
    private List<String> delivered_users;
    private List<String> read_users;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public ACKMessageDTO(){}
    public ACKMessageDTO(String chatid, String tempmsgid, String msgid, String status){
        this.chatid = chatid;
        this.tempmsgid = tempmsgid;
        this.msgid = msgid;
        this.status = status;
    }
    public ACKMessageDTO(String chatid, List<String> msgids, String status, LocalDateTime timestamp){
        this.chatid = chatid;
        this.msgids = msgids;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getchatid(){
        return chatid;
    }
    public void setchatid(String chatid){
        this.chatid = chatid;
    }
    
    public String gettempmsgid(){
        return tempmsgid;
    }
    public void settempmsgid(String tempmsgid){
        this.tempmsgid = tempmsgid;
    }

    public String getmsgid(){
        return msgid;
    }
    public void setmsgid(String msgid){
        this.msgid = msgid;
    }

    public List<String> getmsgids(){
        return msgids;
    }
    public void setmsgids(List<String> msgids){
        this.msgids = msgids;
    }

    public String getstatus(){
        return status;
    }
    public void setstatus(String status){
        this.status = status;
    }

    public List<String> getpending_users(){
        return pending_users;
    }
    public void setpending_users(List<String> pending_users){
        this.pending_users = pending_users;
    }

    public List<String> getdelivered_users(){
        return delivered_users;
    }
    public void setdelivered_users(List<String> delivered_users){
        this.delivered_users = delivered_users;
    }

    public List<String> getread_users(){
        return read_users;
    }
    public void setread_users(List<String> read_users){
        this.read_users = read_users;
    }

    public LocalDateTime gettimestamp(){
        return timestamp;
    }
    public void settimestamp(LocalDateTime timestamp){
        this.timestamp = timestamp;
    }
}


