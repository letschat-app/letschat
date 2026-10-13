package com.letschat.mvp_1.DTOs;

public class UsernameCheckResultDTO {
    private String username;
    private double score;
    private String decision; // "BLOCK", "WARN", "ALLOW"
    private String message;

    public UsernameCheckResultDTO() {}

    public UsernameCheckResultDTO(String username, double score, String decision, String message) {
        this.username = username;
        this.score = score;
        this.decision = decision;
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
