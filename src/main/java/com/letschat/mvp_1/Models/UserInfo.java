package com.letschat.mvp_1.Models;

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
    private String PrivateName;
    @Column("PublicName")
    private String PublicName;
    @Column("UserProfilePath")
    private String UserProfilePath;//null
    private int Age;
    private String Gender;
    private Long Location;
    private Long CurrentLocation;//null
    private String Password;
    @Column("RoomNickName")
    private String RoomNickName;//null
    @Column("Email")
    private String Email;
    @Column("Is2FAEnabled")
    private Boolean is2FAEnabled = false;
    @Column("ShowBusySlotsExternal")
    private Boolean showBusySlotsExternal = false;
    @Column("BookingSlug")
    private String bookingSlug;

    @Transient
    private int pulseStreak;

    public UserInfo(){}

    public UserInfo(String UserId,String PrivateName,String PublicName,String UserProfilePath,int Age,String Gender,Long Location,Long CurrentLocation,String Password,String RoomNickName,String Email){
        this.UserId=UserId;
        this.PrivateName=PrivateName;
        this.PublicName=PublicName;
        this.UserProfilePath=UserProfilePath;
        this.Age=Age;
        this.Gender=Gender;
        this.Location=Location;
        this.CurrentLocation=CurrentLocation;
        this.Password=Password;
        this.RoomNickName=RoomNickName;
        this.Email=Email;
    }

    public String getUserId(){
        return UserId;
    }
    public void setUserId(String UserId){
        this.UserId=UserId;
    }

    public String getPrivateName(){
        return PrivateName;
    }
    public void setPrivateName(String PrivateName){
        this.PrivateName=PrivateName;
    }

    public String getPublicName(){
        return PublicName;
    }
    public void setPublicName(String PublicName){
        this.PublicName=PublicName;
    }

    public String getUserProfilePath(){
        return UserProfilePath;
    }
    public void setUserProfilePath(String UserProfilePath){
        this.UserProfilePath=UserProfilePath;
    }

    public int getAge(){
        return Age;
    }
    public void setAge(int Age){
        this.Age=Age;
    }

    public String getGender(){
        return Gender;
    }
    public void setGender(String Gender){
        this.Gender=Gender;
    }

    public Long getLocation(){
        return Location;
    }
    public void setLocation(Long Location){
        this.Location=Location;
    }

    public Long getCurrentLocation(){
        return CurrentLocation;
    }
    public void setCurrentLocation(Long CurrentLocation){
        this.CurrentLocation=CurrentLocation;
    }

    public String getPassword(){
        return Password;
    }
    public void setPassword(String Password){
        this.Password=Password;
    }

    public String getRoomNickname(){
        return RoomNickName;
    }
    public void setRoomNickName(String RoomNickName){
        this.RoomNickName=RoomNickName;
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
        this.Email=Email;
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
}
