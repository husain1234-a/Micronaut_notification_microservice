package com.yash.notification.dto;

import io.micronaut.serde.annotation.Serdeable;
import java.util.UUID;

@Serdeable
public class UserDeviceDto {
    private UUID id;
    private UUID userId;
    private String fcmToken;
    private String deviceType;
    private String deviceId;

    // Default constructor
    public UserDeviceDto() {}

    // Constructor with all fields
    public UserDeviceDto(UUID id, UUID userId, String fcmToken, String deviceType, String deviceId) {
        this.id = id;
        this.userId = userId;
        this.fcmToken = fcmToken;
        this.deviceType = deviceType;
        this.deviceId = deviceId;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public String toString() {
        return "UserDeviceDto{" +
                "id=" + id +
                ", userId=" + userId +
                ", fcmToken='" + fcmToken + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", deviceId='" + deviceId + '\'' +
                '}';
    }
} 