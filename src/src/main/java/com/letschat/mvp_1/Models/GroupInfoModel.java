package com.letschat.mvp_1.Models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("GroupInfo")
public class GroupInfoModel {
    @Id
    @Column("GroupId")
    private String GroupId;
    @Column("GroupName")
    private String GroupName;
    @Column("GroupProfilePath")
    private String GroupProfilePath;//null
    @Column("CreatedBy")
    private String CreatedBy;
    @Column("ChatId")
    private Long ChatId;
    @Column("NoOfMembers")
    private Integer NoOfMembers;//default 1
    @Column("Motto")
    private String Motto;//null

    public GroupInfoModel(){}
    public GroupInfoModel(String GroupId,String GroupName,String GroupProfilePath,String CreatedBy,Long ChatId,Integer NoOfMembers,String Motto){
        this.GroupId=GroupId;
        this.GroupName=GroupName;
        this.GroupProfilePath=GroupProfilePath;
        this.CreatedBy=CreatedBy;
        this.ChatId=ChatId;
        this.NoOfMembers=NoOfMembers;
        this.Motto=Motto;
    }
    public String getGroupId(){
        return GroupId;
    }
    public void setGroupId(String GroupId){
        this.GroupId=GroupId;
    }

    public String getGroupName(){
        return GroupName;
    }
    public void setGroupName(String GroupName){
        this.GroupName=GroupName;
    }

    public String getGroupProfilePath(){
        return GroupProfilePath;
    }
    public void setGroupProfilePath(String GroupProfilePath){
        this.GroupProfilePath=GroupProfilePath;
    }

    public String getCreatedBy(){
        return CreatedBy;
    }
    public void setCreatedBy(String CreatedBy){
        this.CreatedBy=CreatedBy;
    }

    public Long getChatId(){
        return ChatId;
    }
    public void setChatId(Long ChatId){
        this.ChatId=ChatId;
    }

    public Integer getNoOfMembers(){
        return NoOfMembers;
    }
    public void setNoOfMembers(Integer NoOfMembers){
        this.NoOfMembers=NoOfMembers;
    }

    public String getMotto(){
        return Motto;
    }
    public void setMotto(String Motto){
        this.Motto=Motto;
    }

}
