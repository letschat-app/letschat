package com.letschat.mvp_1.DTOs;

import java.time.LocalDateTime;

public class UserSearchResult {
    private String UserId;
    private String UserName; // Chat Nickname or Active name
    private String username; // Static lowercase public username
    private String displayName; // User's profile display name
    private String Role;
    private String Profile;
    private LocalDateTime lastDisplayNameChangeAt;

    // Universal Pulse Indicators
    private boolean hasActivePulse;
    private boolean allSeen;
    private int pulseStreak;

    public UserSearchResult(){}
    public UserSearchResult(String UserId, String UserName, String Role, String Profile){
        this.UserId = UserId;
        this.UserName = UserName;
        this.Role = Role;
        this.Profile = Profile;
    }

    public UserSearchResult(String UserId, String UserName, String Role, String Profile, boolean hasActivePulse, boolean allSeen, int pulseStreak){
        this.UserId = UserId;
        this.UserName = UserName;
        this.Role = Role;
        this.Profile = Profile;
        this.hasActivePulse = hasActivePulse;
        this.allSeen = allSeen;
        this.pulseStreak = pulseStreak;
    }

    public String getUserId(){
        return UserId;
    }
    public void setUserId(String UserId){
        this.UserId = UserId;
    }

    public String getUserName(){
        return UserName;
    }
    public void setUserName(String UserName){
        this.UserName = UserName;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username != null ? username.trim().toLowerCase() : null;
    }

    public String getDisplayName() {
        return displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public LocalDateTime getLastDisplayNameChangeAt() {
        return lastDisplayNameChangeAt;
    }
    public void setLastDisplayNameChangeAt(LocalDateTime lastDisplayNameChangeAt) {
        this.lastDisplayNameChangeAt = lastDisplayNameChangeAt;
    }

    public String getRole(){
        return Role;
    }
    public void setRole(String Role){
        this.Role = Role;
    }

    public String getProfile(){
        return Profile;
    }
    public void setProfile(String Profile){
        this.Profile = Profile;
    }

    public boolean isHasActivePulse() {
        return hasActivePulse;
    }
    public void setHasActivePulse(boolean hasActivePulse) {
        this.hasActivePulse = hasActivePulse;
    }

    public boolean isAllSeen() {
        return allSeen;
    }
    public void setAllSeen(boolean allSeen) {
        this.allSeen = allSeen;
    }

    public int getPulseStreak() {
        return pulseStreak;
    }
    public void setPulseStreak(int pulseStreak) {
        this.pulseStreak = pulseStreak;
    }
}
