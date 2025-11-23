package com.letschat.mvp_1.DTOs;

public class UserSignUpDTO {
    private String PrivateName;
    private String PublicName;
    private int Age;
    private String Gender;
    private String StateName;
    private String DistrictName;
    private String VillageName;
    private String Password;

    public UserSignUpDTO(){}

    public UserSignUpDTO(String PrivateName,String PublicName,int Age,String Gender,String StateName,String DistrictName,String VillageName,String Password){
        this.PrivateName=PrivateName;
        this.PublicName=PublicName;
        this.Age=Age;
        this.Gender=Gender;
        this.StateName=StateName;
        this.DistrictName=DistrictName;
        this.VillageName=VillageName;
        this.Password=Password;
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

    public String getStateName(){
        return StateName;
    }
    public void setStateName(String StateName){
        this.StateName=StateName;
    }

    public String getDistrictName(){
        return DistrictName;
    }
    public void setDistrictName(String DistrictName){
        this.DistrictName=DistrictName;
    }

    public String getVillageName(){
        return VillageName;
    }
    public void setVillageName(String VillageName){
        this.VillageName=VillageName;
    }

    public String getPassword(){
        return Password;
    }
    public void setPassword(String Password){
        this.Password=Password;
    }
}
