package com.letschat.mvp_1.DTOs;

public class CommunityReturnDTO {
    private String CommunityId;
    private String CommunityName;
    private String ChatId;
    private String motto;
    private Long followers;
    public CommunityReturnDTO() {
    }

    public CommunityReturnDTO(String CommunityId, String CommunityName, String ChatId, String motto,Long followers) {
        this.CommunityId = CommunityId;
        this.CommunityName = CommunityName;
        this.ChatId = ChatId;
        this.motto = motto;
        this.followers = followers;
    }

    // Getter and Setter for CommunityId
    public String getCommunityId() {
        return CommunityId;
    }

    public void setCommunityId(String communityId) {
        this.CommunityId = communityId;
    }

    // Getter and Setter for CommunityName
    public String getCommunityName() {
        return CommunityName;
    }

    public void setCommunityName(String communityName) {
        this.CommunityName = communityName;
    }

    // Getter and Setter for ChatId
    public String getChatId() {
        return ChatId;
    }

    public void setChatId(String chatId) {
        this.ChatId = chatId;
    }

    // Getter and Setter for Motto
    public String getMotto() {
        return motto;
    }

    public void setMotto(String motto) {
        this.motto = motto;
    }

    public Long getFollowers() {
        return followers;
    }

    public void setFollowers(Long followers) {
        this.followers = followers;
    }
}

