package com.letschat.mvp_1.Models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("UserChat")
public class UserChatInfo {
    @Id
    @Column("Id")
    private Long Id;
    @Column("ChatId")
    private String ChatId;
    @Column("UserId")
    private String UserId;
    @Column("ChatName")
    private String ChatName;
    @Column("UserName")
    private String UserName;
    @Column("At")
    private LocalDateTime At;
    @Column("Status")
    private String Status;
    @Column("Visited")
    private LocalDateTime Visited;
    @Column("Type")
    private String Type;
    @Column("Role")
    private String Role;

    public UserChatInfo(){}
    public UserChatInfo(Long Id,String ChatId,String UserId,String ChatName,String UserName,LocalDateTime At,String Status,LocalDateTime Visited,String Type,String Role){
        this.Id=Id;
        this.ChatId=ChatId;
        this.UserId=UserId;
        this.ChatName=ChatName;
        this.UserName=UserName;
        this.At=At;
        this.Status=Status;
        this.Visited=Visited;
        this.Type=Type;
        this.Role=Role;
    }

    public Long getId(){
        return Id;
    }
    public void setId(Long Id){
        this.Id=Id;
    }

    public String getChatId(){
        return ChatId;
    }
    public void setChatId(String ChatId){
        this.ChatId=ChatId;
    }

    public String getUserId(){
        return UserId;
    }
    public void setUserId(String UserId){
        this.UserId=UserId;
    }

    public String getChatName(){
        return ChatName;
    }
    public void setChatName(String ChatName){
        this.ChatName=ChatName;
    }

    public String getUserName(){
        return UserName;
    }
    public void setUserName(String UserName){
        this.UserName=UserName;
    }

    public LocalDateTime getAt(){
        return At;
    }
    public void setAt(LocalDateTime At){
        this.At=At;
    }

    public String getStatus(){
        return Status;
    }
    public void setStatus(String Status){
        this.Status=Status;
    }

    public LocalDateTime getVisited(){
        return Visited;
    }
    public void setVisited(LocalDateTime Visited){
        this.Visited=Visited;
    }

    public String getType(){
        return Type;
    }
    public void setType(String Type){
        this.Type=Type;
    }

    public String getRole(){
        return Role;
    }
    public void setRole(String Role){
        this.Role=Role;
    }

}
