package com.letschat.mvp_1.DTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class ChatBoxReturnDTO {
    private String ChatId;
    private String Id;
    private String ChatName;
    private String Type;
    private String Status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    private String Role;
    private String Profile;
    private Long noOfMembers; 
    private Boolean isMuted;

    public ChatBoxReturnDTO(){}
    public ChatBoxReturnDTO(String ChatId,String Id,String ChatName,String Type,String Status,LocalDateTime timestamp,String Role,String Profile,Long noOfMembers){
        this.ChatId=ChatId;
        this.Id=Id;
        this.ChatName=ChatName;
        this.Type=Type;
        this.Status=Status;
        this.timestamp=timestamp;
        this.Role=Role;
        this.Profile=Profile;
        this.noOfMembers=noOfMembers;
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

    public String getStatus(){
        return Status;
    }
    public void setStatus(String Status){
        this.Status=Status;
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

    public String getProfile(){
        return Profile;
    }
    public void setProfile(String Profile){
        this.Profile=Profile;
    }

    public Long getNoOfMembers(){
        return noOfMembers;
    }
    public void setNoOfMembers(Long noOfMembers){
        this.noOfMembers=noOfMembers;
    }

    private String motto;
    private String description;

    public String getMotto() {
        return motto != null ? motto : (description != null ? description : Status);
    }
    public void setMotto(String motto) {
        this.motto = motto;
    }

    public String getDescription() {
        return description != null ? description : (motto != null ? motto : Status);
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsMuted() {
        return isMuted != null ? isMuted : false;
    }
    public void setIsMuted(Boolean isMuted) {
        this.isMuted = isMuted;
    }
}
