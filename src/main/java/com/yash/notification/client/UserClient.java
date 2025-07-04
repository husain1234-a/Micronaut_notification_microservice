package com.yash.notification.client;

import com.yash.notification.dto.UserDto;
import com.yash.notification.dto.UserDeviceDto;
import com.yash.notification.dto.UserPage;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.annotation.Header;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Client("http://localhost:8080")
public interface UserClient {
    
    @Get("/api/users/{id}")
    Mono<UserDto> getUserById(@PathVariable UUID id, @Header String authorization);
    
    @Get("/api/users")
    Mono<UserPage> getAllUsers(@Header("Authorization") String authorization);
    
    @Get("/api/users")
    Flux<UserDto> getUsersByRole(@QueryValue String role, @Header("Authorization") String authorization);
    
    @Get("/api/users/email/{email}")
    Mono<UserDto> getUserByEmail(@PathVariable String email, @Header("Authorization") String authorization);
    
    @Get("/api/users/{userId}/devices")
    Flux<UserDeviceDto> getUserDevices(@PathVariable UUID userId, @Header("Authorization") String authorization);
} 