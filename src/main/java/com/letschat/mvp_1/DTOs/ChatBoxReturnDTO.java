package com.letschat.mvp_1.DTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class ChatBoxReturnDTO {
    private String ChatId;
    private String Id;
    private String ChatName;
    private String Type;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private String Role;

    public ChatBoxReturnDTO(){}
    public ChatBoxReturnDTO(String ChatId,String Id,String ChatName,String Type,LocalDateTime timestamp,String Role){
        this.ChatId=ChatId;
        this.Id=Id;
        this.ChatName=ChatName;
        this.Type=Type;
        this.timestamp=timestamp;
        this.Role=Role;
    }

    public String getChatId(){
        return ChatId;
    }
    public void setChatId(String ChatId){
        this.ChatId=ChatId;
    }

    public String getId(){
        return Id;
    }
    public void setId(String Id){
        this.Id=Id;
    }

    public String getChatName(){
        return ChatName;
    }
    public void setChatName(String ChatName){
        this.ChatName=ChatName;
    }

    public String getType(){
        return Type;
    }
    public void setType(String Type){
        this.Type=Type;
    }

    public LocalDateTime gettimestamp(){
        return timestamp;
    }
    public void settimestamp(LocalDateTime timestamp){
        this.timestamp=timestamp;
    }

    public String getRole(){
        return Role;
    }
    public void setRole(String Role){
        this.Role=Role;
    }
}
