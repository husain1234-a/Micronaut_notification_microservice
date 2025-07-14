package com.yash.notification.service.impl;

import com.yash.notification.client.UserClient;
import com.yash.notification.dto.UserDeviceDto;
import com.yash.notification.dto.UserDto;
import com.yash.notification.dto.UserPage;
import com.yash.notification.service.UserService;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.context.ServerRequestContext;
import jakarta.inject.Singleton;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Singleton
public class UserServiceImpl implements UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserClient userClient;
    
    public UserServiceImpl(UserClient userClient) {
        this.userClient = userClient;
    }
    
    private String getAuthorizationHeader() {
        return ServerRequestContext.currentRequest()
                .map(request -> request.getHeaders().get(HttpHeaders.AUTHORIZATION))
                .orElse(null);
    }
    
    @Override
    public Mono<UserDto> getUserById(UUID id) {
        log.debug("Fetching user by ID: {}", id);
        try {
            return userClient.getUserById(id, getAuthorizationHeader());
        } catch (Exception e) {
            log.error("Error fetching user by ID: {}", id, e);
            return Mono.error(e);
        }
    }
    
    @Override
    public Flux<UserDto> getAllUsers() {
        log.debug("Fetching all users");
        try {
            return userClient.getAllUsers(getAuthorizationHeader());
        } catch (Exception e) {
            log.error("Error fetching all users", e);
            return Flux.error(e);
        }
    }
    
    @Override
    public Flux<UserDto> getUsersByRole(String role) {
        log.debug("Fetching users by role: {}", role);
        try {
            return userClient.getUsersByRole(role, getAuthorizationHeader());
        } catch (Exception e) {
            log.error("Error fetching users by role: {}", role, e);
            return Flux.error(e);
        }
    }
    
    @Override
    public Mono<UserDto> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        try {
            return userClient.getUserByEmail(email, getAuthorizationHeader());
        } catch (Exception e) {
            log.error("Error fetching user by email: {}", email, e);
            return Mono.error(e);
        }
    }
    
    @Override
    public Flux<UserDeviceDto> getUserDevices(UUID userId) {
        log.debug("Fetching devices for user: {}", userId);
        try {
            return userClient.getUserDevices(userId, getAuthorizationHeader());
        } catch (Exception e) {
            log.error("Error fetching devices for user: {}", userId, e);
            return Flux.error(e);
        }
    }
} 