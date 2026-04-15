package com.letschat.mvp_1.Models;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("\"SubmissionInfo\"")
public class SubmissionInfo {
    @Id
    private Long submissionId;
    private long assignmentId;
    private String comment;
    private String content;
    private String userId;
    private Instant timestamp;
    private Long marks;

    // Default Constructor
    public SubmissionInfo() {
    }

    // Parameterized Constructor
    public SubmissionInfo(Long submissionid, long assignmentId, String comment, String content, 
                          String userId, Instant timestamp, Long marks) {
        this.submissionId = submissionid;
        this.assignmentId = assignmentId;
        this.comment = comment;
        this.content = content;
        this.userId = userId;
        this.timestamp = timestamp;
        this.marks = marks;
    }

    // Getters and Setters

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionid) {
        this.submissionId = submissionid;
    }

    public long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Long getMarks() {
        return marks;
    }

    public void setMarks(Long marks) {
        this.marks = marks;
    }
}