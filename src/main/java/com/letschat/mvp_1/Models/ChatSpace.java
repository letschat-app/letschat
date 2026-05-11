package com.letschat.mvp_1.Models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("\"ChatSpace\"")
public class ChatSpace {
    @Id
    private Long id;
    private String chatId;
    private Long spaceId;
    private String spaceName;

    public ChatSpace(){}
    public ChatSpace(Long id,String chatId,Long spaceId,String spaceName){
        this.chatId=chatId;
        this.spaceId=spaceId;
        this.spaceName=spaceName;
    }

    public void setId(Long id){
        this.id=id;
    }
    public Long getId(){
        return id;
    }

    public void setChatId(String chatId){
        this.chatId=chatId;
    }
    public String getChatId(){
        return chatId;
    }

    public void setSpaceId(Long spaceId){
        this.spaceId=spaceId;
    }
    public Long getSpaceId(){
        return spaceId;
    }

    public void setSpaceName(String spaceName){
        this.spaceName=spaceName;
    }
    public String getSpaceName(){
        return spaceName;
    }
}
