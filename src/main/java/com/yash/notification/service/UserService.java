package com.yash.notification.service;

import com.yash.notification.dto.UserDto;
import com.yash.notification.dto.UserDeviceDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserService {
    Mono<UserDto> getUserById(UUID id);
    Flux<UserDto> getAllUsers();
    Flux<UserDto> getUsersByRole(String role);
    Mono<UserDto> getUserByEmail(String email);
    Flux<UserDeviceDto> getUserDevices(UUID userId);
} 