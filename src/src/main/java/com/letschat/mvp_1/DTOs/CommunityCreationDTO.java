package com.letschat.mvp_1.DTOs;

public class CommunityCreationDTO {
    private String communityName;
    private String motto;
    private String type;

    // Constructor
    public CommunityCreationDTO() {
    }

    public CommunityCreationDTO(String communityName, String motto, String type) {
        this.communityName = communityName;
        this.motto = motto;
        this.type = type;
    }

    public String getCommunityName() {
        return communityName;
    }
    public void setCommunityName(String communityName) {
        this.communityName = communityName;
    }

    public String getMotto() {
        return motto;
    }
    public void setMotto(String motto) {
        this.motto = motto;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
  
}

