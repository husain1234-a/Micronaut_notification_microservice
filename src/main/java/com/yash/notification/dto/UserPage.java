package com.yash.notification.dto;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Serdeable
public class UserPage {
    private List<UserDto> content;
    // Optionally add pageable, totalSize, etc. if needed

    public List<UserDto> getContent() {
        return content;
    }
    public void setContent(List<UserDto> content) {
        this.content = content;
    }
} 