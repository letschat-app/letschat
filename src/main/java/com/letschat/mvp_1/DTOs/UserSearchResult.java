package com.letschat.mvp_1.DTOs;

public class UserSearchResult {
    private String UserId;
    private String UserName;

    public UserSearchResult(){}
    public UserSearchResult(String UserId,String UserName){
        this.UserId=UserId;
        this.UserName=UserName;
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

}
