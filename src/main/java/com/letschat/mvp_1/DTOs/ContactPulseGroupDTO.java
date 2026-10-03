package com.letschat.mvp_1.DTOs;

import java.util.List;

public class ContactPulseGroupDTO {
    private String contactUserId;
    private String contactName;
    private String contactProfile;
    private int pulseStreak;
    private boolean allSeen;
    private List<PulseFeedItemDTO> pulses;

    public ContactPulseGroupDTO() {}

    public String getContactUserId() {
        return contactUserId;
    }
    public void setContactUserId(String contactUserId) {
        this.contactUserId = contactUserId;
    }

    public String getContactName() {
        return contactName;
    }
    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactProfile() {
        return contactProfile;
    }
    public void setContactProfile(String contactProfile) {
        this.contactProfile = contactProfile;
    }

    public int getPulseStreak() {
        return pulseStreak;
    }
    public void setPulseStreak(int pulseStreak) {
        this.pulseStreak = pulseStreak;
    }

    public boolean isAllSeen() {
        return allSeen;
    }
    public void setAllSeen(boolean allSeen) {
        this.allSeen = allSeen;
    }

    public List<PulseFeedItemDTO> getPulses() {
        return pulses;
    }
    public void setPulses(List<PulseFeedItemDTO> pulses) {
        this.pulses = pulses;
    }
}
