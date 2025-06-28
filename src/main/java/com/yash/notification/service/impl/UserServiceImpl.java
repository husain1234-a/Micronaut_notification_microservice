package com.yash.notification.service.impl;

import com.yash.notification.client.UserClient;
import com.yash.notification.dto.UserDto;
import com.yash.notification.dto.UserDeviceDto;
import com.yash.notification.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class UserServiceImpl implements UserService {
    
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserClient userClient;
    
    public UserServiceImpl(UserClient userClient) {
        this.userClient = userClient;
    }
    
    @Override
    public Optional<UserDto> getUserById(UUID id) {
        log.debug("Fetching user by ID: {}", id);
        try {
            return userClient.getUserById(id);
        } catch (Exception e) {
            log.error("Error fetching user by ID: {}", id, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<UserDto> getAllUsers() {
        log.debug("Fetching all users");
        try {
            return userClient.getAllUsers();
        } catch (Exception e) {
            log.error("Error fetching all users", e);
            return List.of();
        }
    }
    
    @Override
    public List<UserDto> getUsersByRole(String role) {
        log.debug("Fetching users by role: {}", role);
        try {
            return userClient.getUsersByRole(role);
        } catch (Exception e) {
            log.error("Error fetching users by role: {}", role, e);
            return List.of();
        }
    }
    
    @Override
    public Optional<UserDto> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        try {
            return userClient.getUserByEmail(email);
        } catch (Exception e) {
            log.error("Error fetching user by email: {}", email, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<UserDeviceDto> getUserDevices(UUID userId) {
        log.debug("Fetching devices for user: {}", userId);
        try {
            return userClient.getUserDevices(userId);
        } catch (Exception e) {
            log.error("Error fetching devices for user: {}", userId, e);
            return List.of();
        }
    }
} 