package com.letschat.mvp_1.Models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("CommunityVideoInfo")
public class CommunityVideoInfo {
    @Id
    @Column("VideoId")
    private Long VideoId;
    @Column("VideoName")
    private String VideoName;
    @Column("VideoLocation")
    private Long VideoLocation;
    @Column("CommunityId")
    private String communityId;
    @Column("VideoPath")
    private String videoPath;
    @Column("Likes")
    private Long likes;
    @Column("DisLikes")
    private Long dislikes;
    @Column("Views")
    private Long views;
    @Column("Description")
    private String description;
    @Column("Type")
    private String Type;

    public CommunityVideoInfo(){}
    public CommunityVideoInfo(Long VideoId,String VideoName,Long VideoLocation,String communityId, String videoPath, Long likes, Long dislikes, Long views, String description,String Type) {
        this.communityId = communityId;
        this.videoPath = videoPath;
        this.likes = likes;
        this.dislikes = dislikes;
        this.views = views;
        this.description = description;
        this.VideoId=VideoId;
        this.VideoName=VideoName;
        this.VideoLocation=VideoLocation;
        this.Type=Type;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }
    
    public Long getVideoId(){
        return VideoId;
    }
    public void setVideoId(Long VideoId){
        this.VideoId=VideoId;
    }

    public String getVideoName(){
        return VideoName;
    }
    public void setVideoName(String VideoName){
        this.VideoName=VideoName;
    }

    public Long getVideoLocation(){
        return VideoLocation;
    }
    public void setVideoLocation(Long VideoLocation){
        this.VideoLocation=VideoLocation;
    }

    public Long getLikes() {
        return likes;
    }

    public void setLikes(Long likes) {
        this.likes = likes;
    }

    public Long getDislikes() {
        return dislikes;
    }

    public void setDislikes(Long dislikes) {
        this.dislikes = dislikes;
    }

    public Long getViews() {
        return views;
    }

    public void setViews(Long views) {
        this.views = views;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCommunityId() {
        return communityId;
    }

    public void setCommunityId(String communityId) {
        this.communityId = communityId;
    }

    public String getType(){
        return Type;
    }

    public void setType(String Type){
        this.Type=Type;
    }
}
