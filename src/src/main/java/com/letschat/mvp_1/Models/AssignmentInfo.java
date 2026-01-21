package com.letschat.mvp_1.Models;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("\"AssignmentInfo\"")
public class AssignmentInfo {
    @Id
    private Long assignmentId;
    private String chatId;
    private String title;
    private String description;
    private String attachments;
    private Instant created;
    private Instant deadline;
    private String status;
    private Integer marks;

    public AssignmentInfo(){}
    public AssignmentInfo(Long assignmentId, String chatId, String title, String description, String attachments, Instant created, Instant deadline, String status,Integer marks){
        this.assignmentId=assignmentId;
        this.chatId=chatId;
        this.title=title;
        this.description=description;
        this.attachments=attachments;
        this.created=created;
        this.deadline=deadline;
        this.status=status;
        this.marks=marks;
    }

    public void setAssignmentId(Long assignmentId){
        this.assignmentId=assignmentId;
    }
    public Long getAssignmentId(){
        return assignmentId;
    }

    public void setChatId(String chatId){
        this.chatId=chatId;
    }
    public String getChatId(){
        return chatId;
    }

    public void setTitle(String title){
        this.title=title;
    }
    public String getTitle(){
        return title;
    }

    public void setDescription(String description){
        this.description=description;
    }
    public String getDescription(){
        return description;
    }

    public void setAttachments(String attachments){
        this.attachments=attachments;
    }
    public String getAttachments(){
        return attachments;
    }

    public void setCreated(Instant created){
        this.created=created;
    }
    public Instant getCreated(){
        return created;
    }

    public void setDeadline(Instant deadline){
        this.deadline=deadline;
    }
    public Instant getDeadline(){
        return deadline;
    }

    public void setStatus(String status){
        this.status=status;
    }
    public String getStatus(){
        return status;
    }

    public void setMarks(Integer marks){
        this.marks=marks;
    }
    public Integer getMarks(){
        return marks;
    }
}
