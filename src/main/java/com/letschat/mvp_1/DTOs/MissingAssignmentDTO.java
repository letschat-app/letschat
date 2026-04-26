package com.letschat.mvp_1.DTOs;

import java.util.List;

public class MissingAssignmentDTO {

    private String userId;
    private List<Long> assignmentIds;

    public MissingAssignmentDTO() {}

    public MissingAssignmentDTO(String userId, List<Long> assignmentIds) {
        this.userId = userId;
        this.assignmentIds = assignmentIds;
    }

    public String getUserId() {
        return userId;
    }

    public List<Long> getAssignmentIds() {
        return assignmentIds;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setAssignmentIds(List<Long> assignmentIds) {
        this.assignmentIds = assignmentIds;
    }
}