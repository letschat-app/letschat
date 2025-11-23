package com.letschat.mvp_1.DTOs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ChatUpdateDTO {
    private String purpose;
    @JsonProperty("userchatId")
    private String userchatId;
    @JsonProperty("usermsgId")
    private String usermsgId;

    public ChatUpdateDTO(){}
    public ChatUpdateDTO(String purpose,String userchatId,String msgId){
        this.purpose=purpose;
        this.userchatId=userchatId;
        this.usermsgId=msgId;
    }

    public String getpurpose(){
        return purpose;
    }
    public void setpurpose(String purpose){
        this.purpose=purpose;
    }
    
    public String getuserchatid(){
        return userchatId;
    }
    public void setuserchatid(String chatid){
        this.userchatId=chatid;
    }
    
    public String getmsgid(){
        return usermsgId;
    }
    public void setmsgid(String msgid){
        this.usermsgId=msgid;
    }
}
