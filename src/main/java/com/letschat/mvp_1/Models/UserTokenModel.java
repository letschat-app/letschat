package com.letschat.mvp_1.Models;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("\"UserFCMToken\"")
public class UserTokenModel {

    private String userId;
    @Id
    private String deviceId;
    private String fcmToken;

    // Default Constructor
    public UserTokenModel() {
    }

    // Parameterized Constructor
    public UserTokenModel(String userId, String deviceId, String fcmToken) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.fcmToken = fcmToken;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}