package com.letschat.mvp_1.Models;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Table("\"ClassRoomInfo\"")
public class ClassRoomInfo implements Persistable<String>{
    @Id
    private String roomId;
    private String roomName;
    private Long chatId;
    private String faculty;
    private Long noOfStudents;
    private String description;
    private Long roomProfile;

    @Override
    public String getId(){
        return roomId;
    }

    @Override
    public boolean isNew(){
        return true;
    }

    public ClassRoomInfo(){}

    public ClassRoomInfo(String roomId, String roomName, Long chatId,
                   String faculty, Long noOfStudents,
                   String description, Long roomProfile) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.chatId=chatId;
        this.faculty = faculty;
        this.noOfStudents = noOfStudents;
        this.description = description;
        this.roomProfile = roomProfile;
    }

    public String getRoomId() {
        return roomId;
    }
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }
    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Long getChatId() {
        return chatId;
    }
    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getFaculty() {
        return faculty;
    }
    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public Long getNoOfStudents() {
        return noOfStudents;
    }
    public void setNoOfStudents(Long noOfStudents) {
        this.noOfStudents = noOfStudents;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public Long getRoomProfile() {
        return roomProfile;
    }
    public void setRoomProfile(Long roomProfile) {
        this.roomProfile = roomProfile;
    }
}
