package com.letschat.mvp_1.DTOs;

public class UserLoginDTO {
    private String userid;
    private String password;

    public UserLoginDTO(){}

    public UserLoginDTO(String userid,String password){
        this.userid=userid;
        this.password=password;
    }

    public String getUserId(){
        return userid;
    }
    public void setUserId(String userid){
        this.userid=userid;
    }

    public String getPassword(){
        return password;
    }
    public void setPassword(String password){
        this.password=password;
    }
}
