package com.yash.notification.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

@Serdeable
public class CreateNotificationRequest {
    private UUID userId;
    private String title;
    private String message;

    public CreateNotificationRequest(UUID userId, String title, String message) {
        this.userId = userId;
        this.title = title;
        this.message = message;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }}