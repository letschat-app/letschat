package com.letschat.mvp_1.DTOs;

public class ACKMessageDTO {
    private String chatid;
    private String tempmsgid;
    private String msgid;
    private String status;
    
    public ACKMessageDTO(){}
    public ACKMessageDTO(String chatid,String tempmsgid,String msgid,String status){
        this.chatid=chatid;
        this.tempmsgid=tempmsgid;
        this.msgid=msgid;
        this.status=status;
    }

    public String getchatid(){
        return chatid;
    }
    public void setchatid(String chatid){
        this.chatid=chatid;
    }
    
    public String gettempmsgid(){
        return tempmsgid;
    }
    public void settempmsgid(String tempmsgid){
        this.tempmsgid=tempmsgid;
    }

    public String getmsgid(){
        return msgid;
    }
    public void setmsgid(String msgid){
        this.msgid=msgid;
    }

    public String getstatus(){
        return status;
    }
    public void setstatus(String status){
        this.status=status;
    }
}
