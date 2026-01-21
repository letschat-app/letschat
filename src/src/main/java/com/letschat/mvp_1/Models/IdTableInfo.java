package com.letschat.mvp_1.Models;

import org.springframework.data.relational.core.mapping.Table;

@Table("IdTable")
public class IdTableInfo {
    private Long UserId;
    private Long GroupId;
    private Long ChatId;

    public IdTableInfo(){}
    public IdTableInfo(Long UserId,Long GroupId,Long ChatId){
        this.UserId=UserId;
        this.GroupId=GroupId;
        this.ChatId=ChatId;
    }

    public Long getUserId(){
        return UserId;
    }
    public void setUserId(Long UserId){
        this.UserId=UserId;
    }

    public Long getGroupId(){
        return GroupId;
    }
    public void setGroupId(Long GroupId){
        this.GroupId=GroupId;
    }

    public Long getChatId(){
        return ChatId;
    }
    public void setChatId(Long ChatId){
        this.ChatId=ChatId;
    }
}
