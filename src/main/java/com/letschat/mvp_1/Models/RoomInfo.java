package com.letschat.mvp_1.Models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("\"Room\"")
public class RoomInfo {

    @Id
    private String roomId;
    private String roomName;
    private String profile;
    private String creator;
    private Long chatId;
    private Long noOfMembers;
    private String motto;

    // Default Constructor
    public RoomInfo() {
    }

    // Full Constructor
    public RoomInfo(String roomId, String roomName, String profile, String creator,
                    Long chatId, Long noOfMembers, String motto) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.profile = profile;
        this.creator = creator;
        this.chatId = chatId;
        this.noOfMembers = noOfMembers;
        this.motto = motto;
    }

    // Getters and Setters

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getNoOfMembers() {
        return noOfMembers;
    }

    public void setNoOfMembers(Long noOfMembers) {
        this.noOfMembers = noOfMembers;
    }

    public String getMotto() {
        return motto;
    }

    public void setMotto(String motto) {
        this.motto = motto;
    }
}