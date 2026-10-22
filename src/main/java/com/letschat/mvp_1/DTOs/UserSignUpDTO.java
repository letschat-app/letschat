package com.letschat.mvp_1.DTOs;

public class UserSignUpDTO {
    private String PrivateName; // DisplayName
    private String PublicName;  // Username
    private String Password;
    private String Email;
    private String Otp;

    public UserSignUpDTO(){}

    public UserSignUpDTO(String PrivateName, String PublicName, String Password, String Email){
        this.PrivateName = PrivateName;
        this.PublicName = PublicName;
        this.Password = Password;
        this.Email = Email;
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

    public String getPassword(){
        return Password;
    }
    public void setPassword(String Password){
        this.Password = Password;
    }

    public String getEmail(){
        return Email;
    }
    public void setEmail(String Email){
        this.Email = Email;
    }

    public String getOtp(){
        return Otp;
    }
    public void setOtp(String Otp){
        this.Otp = Otp;
    }
}
