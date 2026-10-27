package com.letschat.mvp_1.Models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("\"UserInfo\"")
public class UserInfo {
    @Id
    @Column("UserId")
    private String UserId;
    @Column("PrivateName")
    private String PrivateName; // Represents editable DisplayName
    @Column("PublicName")
    private String PublicName;   // Represents static lowercase Username
    @Column("UserProfilePath")
    private String UserProfilePath;
    private String Password;
    @Column("RoomNickName")
    private String RoomNickName;
    @Column("Email")
    private String Email;
    @Column("Is2FAEnabled")
    private Boolean is2FAEnabled = false;
    @Column("ShowBusySlotsExternal")
    private Boolean showBusySlotsExternal = false;
    @Column("BookingSlug")
    private String bookingSlug;
    @Column("IsBookingEnabled")
    private Boolean isBookingEnabled = true;

    @Column("LastDisplayNameChangeAt")
    private LocalDateTime lastDisplayNameChangeAt;

    @Transient
    private int pulseStreak;

    public UserInfo(){}

    public UserInfo(String UserId, String PrivateName, String PublicName, String UserProfilePath, String Password, String RoomNickName, String Email, LocalDateTime lastDisplayNameChangeAt){
        this.UserId = UserId;
        this.PrivateName = PrivateName;
        this.PublicName = PublicName;
        this.UserProfilePath = UserProfilePath;
        this.Password = Password;
        this.RoomNickName = RoomNickName;
        this.Email = Email;
        this.lastDisplayNameChangeAt = lastDisplayNameChangeAt;
    }

    public String getUserId(){
        return UserId;
    }
    public void setUserId(String UserId){
        this.UserId = UserId;
    }

    public String getPrivateName(){
        return PrivateName;
    }
    public void setPrivateName(String PrivateName){
        this.PrivateName = PrivateName;
    }

    public String getDisplayName() {
        return PrivateName;
    }
    public void setDisplayName(String displayName) {
        this.PrivateName = displayName;
    }

    public String getPublicName(){
        return PublicName;
    }
    public void setPublicName(String PublicName){
        this.PublicName = PublicName != null ? PublicName.trim().toLowerCase() : null;
    }

    public String getUsername() {
        return PublicName;
    }
    public void setUsername(String username) {
        this.PublicName = username != null ? username.trim().toLowerCase() : null;
    }

    public String getUserProfilePath(){
        return UserProfilePath;
    }
    public void setUserProfilePath(String UserProfilePath){
        this.UserProfilePath = UserProfilePath;
    }

    public String getProfilePic() {
        return UserProfilePath;
    }

    public String getPassword(){
        return Password;
    }
    public void setPassword(String Password){
        this.Password = Password;
    }

    public String getRoomNickname(){
        return RoomNickName;
    }
    public void setRoomNickName(String RoomNickName){
        this.RoomNickName = RoomNickName;
    }

    public int getPulseStreak() {
        return pulseStreak;
    }
    public void setPulseStreak(int pulseStreak) {
        this.pulseStreak = pulseStreak;
    }

    public String getEmail(){
        return Email;
    }
    public void setEmail(String Email){
        this.Email = Email;
    }

    public Boolean getIs2FAEnabled() {
        return is2FAEnabled != null && is2FAEnabled;
    }
    public void setIs2FAEnabled(Boolean is2FAEnabled) {
        this.is2FAEnabled = is2FAEnabled;
    }

    public Boolean getShowBusySlotsExternal() {
        return showBusySlotsExternal != null && showBusySlotsExternal;
    }
    public void setShowBusySlotsExternal(Boolean showBusySlotsExternal) {
        this.showBusySlotsExternal = showBusySlotsExternal;
    }

    public String getBookingSlug() {
        return bookingSlug;
    }
    public void setBookingSlug(String bookingSlug) {
        this.bookingSlug = bookingSlug;
    }

    public Boolean getIsBookingEnabled() {
        return isBookingEnabled == null || isBookingEnabled;
    }
    public void setIsBookingEnabled(Boolean isBookingEnabled) {
        this.isBookingEnabled = isBookingEnabled;
    }

    public LocalDateTime getLastDisplayNameChangeAt() {
        return lastDisplayNameChangeAt;
    }
    public void setLastDisplayNameChangeAt(LocalDateTime lastDisplayNameChangeAt) {
        this.lastDisplayNameChangeAt = lastDisplayNameChangeAt;
    }


    public String getUserName() { return getUsername(); }
    public String getName() { return getDisplayName(); }
    public String getProfile() { return getUserProfilePath(); }

}
