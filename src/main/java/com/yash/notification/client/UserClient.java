package com.yash.notification.client;

import com.yash.notification.dto.UserDto;
import com.yash.notification.dto.UserDeviceDto;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.annotation.QueryValue;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Client("${user.service.url:http://localhost:8080}")
public interface UserClient {
    
    @Get("/api/users/{id}")
    Optional<UserDto> getUserById(@PathVariable UUID id);
    
    @Get("/api/users")
    List<UserDto> getAllUsers();
    
    @Get("/api/users")
    List<UserDto> getUsersByRole(@QueryValue String role);
    
    @Get("/api/users/email/{email}")
    Optional<UserDto> getUserByEmail(@PathVariable String email);
    
    @Get("/api/users/{userId}/devices")
    List<UserDeviceDto> getUserDevices(@PathVariable UUID userId);
} 