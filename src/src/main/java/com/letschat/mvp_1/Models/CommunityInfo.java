package com.letschat.mvp_1.Models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("CommunityInfo")
public class CommunityInfo {
    @Id
    @Column("CommunityId")
    private String CommunityId;
    @Column("CommunityName")
    private String CommunityName;
    @Column("CommunityProfilePath")
    private String CommunityProfilePath;
    @Column("CommunityLocation")
    private String CommunityLocation;
    @Column("Creator")
    private String Creator;
    @Column("ChatId")
    private Long ChatId;
    @Column("NoOfMembers")
    private Long NoOfMembers;
    @Column("Motto")
    private String Motto;
    @Column("TotalVideo")
    private Long TotalVideo;
    @Column("TotalViews")
    private Long TotalViews;
    @Column("TotalAmountRecived")
    private Long TotalAmountRecived;
    @Column("Type")
    private String Type;

    public CommunityInfo(){}
    public CommunityInfo(String CommunityId,String CommunityName,String CommunityProfilePath,String CommunityLocation,
    String Creator,Long ChatId,Long NoOfMembers,String Motto,Long TotalVideo,Long TotalViews,Long TotalAmountRecived,String Type){
        this.CommunityId=CommunityId;
        this.CommunityName=CommunityName;
        this.CommunityProfilePath=CommunityProfilePath;
        this.CommunityLocation=CommunityLocation;
        this.Creator=Creator;
        this.ChatId=ChatId;
        this.NoOfMembers=NoOfMembers;
        this.Motto=Motto;
        this.TotalVideo=TotalVideo;
        this.TotalViews=TotalViews;
        this.TotalAmountRecived=TotalAmountRecived;
        this.Type=Type;
    }

    public String getCommunityId(){
        return CommunityId;
    }
    public void setCommunityId(String CommunityId){
        this.CommunityId=CommunityId;
    }

    public String getCommunityName(){
        return CommunityName;
    }
    public void setCommunityName(String CommunityName){
        this.CommunityName=CommunityName;
    }

    public String getCommunityProfilePath(){
        return CommunityProfilePath;
    }
    public void setCommunityProfilePath(String CommunityProfilePath){
        this.CommunityProfilePath=CommunityProfilePath;
    }

    public String getCommunityLocation(){
        return CommunityLocation;
    }
    public void setCommunityLocation(String CommunityLocation){
        this.CommunityLocation=CommunityLocation;
    }

    public String getCreator(){
        return Creator;
    }
    public void setCreator(String Creator){
        this.Creator=Creator;
    }

    public Long getChatId(){
        return ChatId;
    }
    public void setChatId(Long ChatId){
        this.ChatId=ChatId;
    }

    public Long getNoOfMembers(){
        return NoOfMembers;
    }
    public void setNoOfMembers(Long NoOfMembers){
        this.NoOfMembers=NoOfMembers;
    }

    public String getMotto(){
        return Motto;
    }
    public void setMotto(String Motto){
        this.Motto=Motto;
    }

    public Long getTotalVideo(){
        return TotalVideo;
    }
    public void setTotalVideo(Long TotalVideo){
        this.TotalVideo=TotalVideo;
    }

    public Long getTotalViews(){
        return TotalViews;
    }
    public void setTotalViews(Long TotalViews){
        this.TotalViews=TotalViews;
    }

    public Long getTotalAmountRecived(){
        return TotalAmountRecived;
    }
    public void setTotalAmountRecived(Long TotalAmountRecived){
        this.TotalAmountRecived=TotalAmountRecived;
    }

    public String getType(){
        return Type;
    }
    public void setType(String Type){
        this.Type=Type;
    }
}
