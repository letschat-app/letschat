package com.letschat.mvp_1.DTOs;

public class ChatBoxReturnDTO {
    private String ChatId;
    private String Id;
    private String ChatName;
    private String Type;

    public ChatBoxReturnDTO(){}
    public ChatBoxReturnDTO(String ChatId,String Id,String ChatName,String Type){
        this.ChatId=ChatId;
        this.Id=Id;
        this.ChatName=ChatName;
        this.Type=Type;
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
}
