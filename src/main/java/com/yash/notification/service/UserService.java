package com.yash.notification.service;

import com.yash.notification.dto.UserDto;
import com.yash.notification.dto.UserDeviceDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {
    Optional<UserDto> getUserById(UUID id);
    List<UserDto> getAllUsers();
    List<UserDto> getUsersByRole(String role);
    Optional<UserDto> getUserByEmail(String email);
    List<UserDeviceDto> getUserDevices(UUID userId);
} 