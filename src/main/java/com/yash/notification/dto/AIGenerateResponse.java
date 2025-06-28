package com.yash.notification.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class AIGenerateResponse {
    private String generatedMessage;

    public AIGenerateResponse(String generatedMessage) {
        this.generatedMessage = generatedMessage;
    }

    public String getGeneratedMessage() {
        return generatedMessage;
    }

    public void setGeneratedMessage(String generatedMessage) {
        this.generatedMessage = generatedMessage;
    }
}