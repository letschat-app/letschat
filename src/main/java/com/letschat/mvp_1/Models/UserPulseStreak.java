package com.letschat.mvp_1.Models;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("\"UserPulseStreak\"")
public class UserPulseStreak {
    @Id
    @Column("user_id")
    private String userId;

    @Column("current_streak")
    private Integer currentStreak;

    @Column("last_pulse_date")
    private LocalDate lastPulseDate;

    public UserPulseStreak() {}

    public UserPulseStreak(String userId, Integer currentStreak, LocalDate lastPulseDate) {
        this.userId = userId;
        this.currentStreak = currentStreak;
        this.lastPulseDate = lastPulseDate;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getCurrentStreak() {
        return currentStreak;
    }
    public void setCurrentStreak(Integer currentStreak) {
        this.currentStreak = currentStreak;
    }

    public LocalDate getLastPulseDate() {
        return lastPulseDate;
    }
    public void setLastPulseDate(LocalDate lastPulseDate) {
        this.lastPulseDate = lastPulseDate;
    }

    public int getEffectiveStreak(LocalDate today) {
        if (this.currentStreak == null || this.lastPulseDate == null) {
            return 0;
        }
        long daysPassed = java.time.temporal.ChronoUnit.DAYS.between(this.lastPulseDate, today);
        if (daysPassed <= 1) {
            return Math.max(0, this.currentStreak);
        }
        long missedDays = daysPassed - 1;
        return (int) Math.max(0, this.currentStreak - missedDays);
    }
}
