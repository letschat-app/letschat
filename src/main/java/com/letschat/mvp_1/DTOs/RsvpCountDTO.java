package com.letschat.mvp_1.DTOs;

public class RsvpCountDTO {
    private String rsvpStatus;
    private Long count;

    public RsvpCountDTO() {}

    public RsvpCountDTO(String rsvpStatus, Long count) {
        this.rsvpStatus = rsvpStatus;
        this.count = count;
    }

    public String getRsvpStatus() { return rsvpStatus; }
    public void setRsvpStatus(String rsvpStatus) { this.rsvpStatus = rsvpStatus; }

    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
}
