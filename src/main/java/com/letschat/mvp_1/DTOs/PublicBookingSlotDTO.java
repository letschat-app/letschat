package com.letschat.mvp_1.DTOs;

import java.time.Instant;

public class PublicBookingSlotDTO {
    private Instant startTime;
    private Instant endTime;
    private String status = "busy";

    public PublicBookingSlotDTO() {}

    public PublicBookingSlotDTO(Instant startTime, Instant endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = "busy";
    }

    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }

    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
