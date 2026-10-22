package com.letschat.mvp_1.DTOs;

import java.time.Instant;

public class PublicBookingRequestDTO {
    private String agenda;
    private String bookerName;
    private String bookerEmail;
    private Instant startTime;
    private Instant endTime;
    private String meetingType; // "online" or "offline"
    private String meetingLink;
    private String location;
    private String guestUserId;

    public PublicBookingRequestDTO() {}

    public String getAgenda() { return agenda; }
    public void setAgenda(String agenda) { this.agenda = agenda; }

    public String getBookerName() { return bookerName; }
    public void setBookerName(String bookerName) { this.bookerName = bookerName; }

    public String getBookerEmail() { return bookerEmail; }
    public void setBookerEmail(String bookerEmail) { this.bookerEmail = bookerEmail; }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public String getMeetingType() { return meetingType; }
    public void setMeetingType(String meetingType) { this.meetingType = meetingType; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getGuestUserId() { return guestUserId; }
    public void setGuestUserId(String guestUserId) { this.guestUserId = guestUserId; }
}
