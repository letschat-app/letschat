package com.letschat.mvp_1.DTOs;

public class UserSearchResult {
    private String UserId;
    private String UserName;
    private String Role;

    public UserSearchResult(){}
    public UserSearchResult(String UserId,String UserName,String Role){
        this.UserId=UserId;
        this.UserName=UserName;
        this.Role=Role;
    }

    public String getUserId(){
        return UserId;
    }
    public void setUserId(String UserId){
        this.UserId=UserId;
    }

    public String getUserName(){
        return UserName;
    }
    public void setUserName(String UserName){
        this.UserName=UserName;
    }

    public String getRole(){
        return Role;
    }
    public void setRole(String Role){
        this.Role=Role;
    }

}
