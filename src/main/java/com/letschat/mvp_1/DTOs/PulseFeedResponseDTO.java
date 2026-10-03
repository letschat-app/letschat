package com.letschat.mvp_1.DTOs;

import java.util.List;

public class PulseFeedResponseDTO {
    private String viewerUserId;
    private int contactsCount;
    private List<ContactPulseGroupDTO> feed;

    public PulseFeedResponseDTO() {}

    public String getViewerUserId() {
        return viewerUserId;
    }
    public void setViewerUserId(String viewerUserId) {
        this.viewerUserId = viewerUserId;
    }

    public int getContactsCount() {
        return contactsCount;
    }
    public void setContactsCount(int contactsCount) {
        this.contactsCount = contactsCount;
    }

    public List<ContactPulseGroupDTO> getFeed() {
        return feed;
    }
    public void setFeed(List<ContactPulseGroupDTO> feed) {
        this.feed = feed;
    }
}
