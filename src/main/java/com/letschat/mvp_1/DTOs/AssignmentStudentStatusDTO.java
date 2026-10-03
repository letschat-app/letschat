package com.letschat.mvp_1.DTOs;

import java.time.Instant;

public class AssignmentStudentStatusDTO {
    private String userId;
    private String userName;
    private String profilePic;
    private String status; // "SUBMITTED" or "MISSING"
    private Instant submittedAt;
    private Long submissionId;
    private String content;
    private String comment;
    private Long marks;

    public AssignmentStudentStatusDTO() {}

    public AssignmentStudentStatusDTO(String userId, String userName, String profilePic, String status, Instant submittedAt, Long submissionId, String content, String comment, Long marks) {
        this.userId = userId;
        this.userName = userName;
        this.profilePic = profilePic;
        this.status = status;
        this.submittedAt = submittedAt;
        this.submissionId = submissionId;
        this.content = content;
        this.comment = comment;
        this.marks = marks;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Long getMarks() { return marks; }
    public void setMarks(Long marks) { this.marks = marks; }
}
