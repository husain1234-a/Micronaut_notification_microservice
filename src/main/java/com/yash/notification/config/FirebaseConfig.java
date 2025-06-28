package com.yash.notification.config;

import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("firebase")
public class FirebaseConfig {

    private String serviceAccountKeyPath;

    public String getServiceAccountKeyPath() {
        return serviceAccountKeyPath;
    }

    public void setServiceAccountKeyPath(String serviceAccountKeyPath) {
        this.serviceAccountKeyPath = serviceAccountKeyPath;
    }
} 