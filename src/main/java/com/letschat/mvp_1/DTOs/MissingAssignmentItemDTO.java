package com.letschat.mvp_1.DTOs;

import java.time.Instant;

public class MissingAssignmentItemDTO {
    private Long assignmentId;
    private String title;
    private Instant deadline;
    private String chatName;
    private String chatId;

    public MissingAssignmentItemDTO() {}

    public MissingAssignmentItemDTO(Long assignmentId, String title, Instant deadline, String chatName) {
        this.assignmentId = assignmentId;
        this.title = title;
        this.deadline = deadline;
        this.chatName = chatName;
    }

    public MissingAssignmentItemDTO(Long assignmentId, String title, Instant deadline, String chatName, String chatId) {
        this.assignmentId = assignmentId;
        this.title = title;
        this.deadline = deadline;
        this.chatName = chatName;
        this.chatId = chatId;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public void setAssignment_id(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public void setChat_name(String chatName) {
        this.chatName = chatName;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public void setChat_id(String chatId) {
        this.chatId = chatId;
    }
}
