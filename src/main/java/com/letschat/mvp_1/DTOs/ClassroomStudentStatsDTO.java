package com.letschat.mvp_1.DTOs;

public class ClassroomStudentStatsDTO {
    private String userId;
    private String userName;
    private String profilePic;
    private int completedAssignments;
    private int pendingAssignments;
    private int totalAssignments;
    private double completionPercentage;

    public ClassroomStudentStatsDTO() {}

    public ClassroomStudentStatsDTO(String userId, String userName, String profilePic, int completedAssignments, int pendingAssignments, int totalAssignments, double completionPercentage) {
        this.userId = userId;
        this.userName = userName;
        this.profilePic = profilePic;
        this.completedAssignments = completedAssignments;
        this.pendingAssignments = pendingAssignments;
        this.totalAssignments = totalAssignments;
        this.completionPercentage = completionPercentage;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    public int getCompletedAssignments() { return completedAssignments; }
    public void setCompletedAssignments(int completedAssignments) { this.completedAssignments = completedAssignments; }

    public int getPendingAssignments() { return pendingAssignments; }
    public void setPendingAssignments(int pendingAssignments) { this.pendingAssignments = pendingAssignments; }

    public int getTotalAssignments() { return totalAssignments; }
    public void setTotalAssignments(int totalAssignments) { this.totalAssignments = totalAssignments; }

    public double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(double completionPercentage) { this.completionPercentage = completionPercentage; }
}
