package com.letschat.mvp_1.DTOs;

public class UserSearchResult {
    private String UserId;
    private String UserName;
    private String Role;
    private String Profile;

    // 🟢 Universal Pulse Indicators
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
