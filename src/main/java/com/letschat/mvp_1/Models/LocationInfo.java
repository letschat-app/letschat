package com.letschat.mvp_1.Models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;


@Table("LocationInfo")
public class LocationInfo {
    @Id
    @Column("LocationId")
    private Long LocationId;
    @Column("StateName")
    private String StateName;
    @Column("DistrictName")
    private String DistrictName;
    @Column("VillageName")
    private String VillageName;
    @Column("Latitude")
    private Double Latitude;
    @Column("Longtitude")
    private Double Longtitude;

    public LocationInfo(){}
    public LocationInfo(Long LocationId,String StateName,String DistrictName,String VillageName,Double Latitude,Double Longtitude){
        this.LocationId=LocationId;
        this.StateName=StateName;
        this.DistrictName=DistrictName;
        this.VillageName=VillageName;
        this.Latitude=Latitude;
        this.Longtitude=Longtitude;
    }
    public Long getLocationId(){
        return LocationId;
    }
    public void setLocationId(Long LocationId){
        this.LocationId=LocationId;
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

    public Double getLatitude(){
        return Latitude;
    }
    public void setLatitude(Double Latitude){
        this.Latitude=Latitude;
    }

    public Double getLongtitude(){
        return Longtitude;
    }
    public void setLongtitude(Double Longtitude){
        this.Longtitude=Longtitude;
    }

}
