package com.letschat.mvp_1.DTOs;

import java.util.List;

public class MyPulsesResponseDTO {
    private String userId;
    private int pulseStreak;
    private int activeCount;
    private int expiredCount;
    private List<MyPulseDetailDTO> pulses;

    public MyPulsesResponseDTO() {}

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getPulseStreak() {
        return pulseStreak;
    }
    public void setPulseStreak(int pulseStreak) {
        this.pulseStreak = pulseStreak;
    }

    public int getActiveCount() {
        return activeCount;
    }
    public void setActiveCount(int activeCount) {
        this.activeCount = activeCount;
    }

    public int getExpiredCount() {
        return expiredCount;
    }
    public void setExpiredCount(int expiredCount) {
        this.expiredCount = expiredCount;
    }

    public List<MyPulseDetailDTO> getPulses() {
        return pulses;
    }
    public void setPulses(List<MyPulseDetailDTO> pulses) {
        this.pulses = pulses;
    }
}
