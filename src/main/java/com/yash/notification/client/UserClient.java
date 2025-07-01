package com.yash.notification.client;

import com.yash.notification.dto.UserDto;
import com.yash.notification.dto.UserDeviceDto;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.annotation.Header;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Client("http://localhost:8080")
public interface UserClient {
    
    @Get("/api/users/{id}")
    Optional<UserDto> getUserById(@PathVariable UUID id, @Header("Authorization") String authorization);
    
    @Get("/api/users")
    List<UserDto> getAllUsers(@Header("Authorization") String authorization);
    
    @Get("/api/users")
    List<UserDto> getUsersByRole(@QueryValue String role, @Header("Authorization") String authorization);
    
    @Get("/api/users/email/{email}")
    Optional<UserDto> getUserByEmail(@PathVariable String email, @Header("Authorization") String authorization);
    
    @Get("/api/users/{userId}/devices")
    List<UserDeviceDto> getUserDevices(@PathVariable UUID userId, @Header("Authorization") String authorization);
} 