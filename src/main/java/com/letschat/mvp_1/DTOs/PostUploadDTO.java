package com.letschat.mvp_1.DTOs;

public class PostUploadDTO {
    private String communityId;
    private String videoName;
    private String description;
    
    public PostUploadDTO(){}
    public PostUploadDTO(String communityId, String videoName, String description) {
        this.communityId = communityId;
        this.videoName = videoName;
        this.description = description;
    }

    public String getCommunityId() {
        return communityId;
    }

    public void setCommunityId(String communityId) {
        this.communityId = communityId;
    }

    // Getter and Setter for videoName
    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    // Getter and Setter for description
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
