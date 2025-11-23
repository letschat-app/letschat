package com.letschat.mvp_1.DTOs;

public class PostFeedDTO {
    private String VideoName;
    private String description;

    private String communityname;
    private Long likes;
    private Long dislikes;
    private String videoPath;
    private String type;

    public PostFeedDTO(){}
    public PostFeedDTO(String videoName,String description,String CommunityName,Long likes,Long dislikes,String videoPath,String type){
        this.VideoName=videoName;
        this.description=description;
        this.communityname=CommunityName;
        this.likes=likes;
        this.dislikes=dislikes;
        this.videoPath=videoPath;
        this.type=type;

    }

    public void setVideoName(String videoname){
        this.VideoName=videoname;
    }
    public String getVideoName(){
        return VideoName;
    }

    public void setDescription(String desc){
        this.description=desc;
    }
    public String getDescription(){
        return description;
    }

    public void setCommunityName(String communityname){
        this.communityname=communityname;
    }
    public String getCommunityName(){
        return communityname;
    }

    public void setLikes(Long likes){
        this.likes=likes;
    }
    public Long getLikes(){
        return likes;
    }

    public void setDislikes(Long dislikes){
        this.dislikes=dislikes;
    }
    public Long getDislikes(){
        return dislikes;
    }

    public void setVideoPath(String vpath){
        this.videoPath=vpath;
    }
    public String getVideoPath(){
        return videoPath;
    }

    public void setType(String type){
        this.type=type;
    }
    public String getType(){
        return type;
    }
}
