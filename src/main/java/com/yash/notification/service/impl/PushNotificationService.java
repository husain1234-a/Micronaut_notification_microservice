package com.yash.notification.service.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.yash.notification.model.Notification;
import com.yash.notification.model.NotificationPriority;
import com.yash.notification.dto.UserDto;
import com.yash.notification.dto.UserDeviceDto;
import com.yash.notification.repository.NotificationRepository;
import com.yash.notification.service.NotificationService;
import com.yash.notification.service.UserService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Named("push")
public class PushNotificationService implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final FirebaseMessaging firebaseMessaging;
    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public PushNotificationService(FirebaseMessaging firebaseMessaging,
                                 NotificationRepository notificationRepository,
                                 UserService userService) {
        this.firebaseMessaging = firebaseMessaging;
        this.notificationRepository = notificationRepository;
        this.userService = userService;
    }

    @Override
    public Mono<Notification> createNotification(Notification notification) {
        log.info("Creating push notification for user {}", notification.getUserId());
        return notificationRepository.save(notification)
            .flatMap(savedNotification ->
                userService.getUserDevices(notification.getUserId())
                    .collectList()
                    .flatMapMany(userDevices -> {
                        if (userDevices.isEmpty()) {
                            log.warn("No devices found for user {}. Cannot send push notification.", notification.getUserId());
                            return Flux.empty();
                        }
                        return Flux.fromIterable(userDevices)
                            .flatMap(device -> Mono.fromCallable(() -> {
                                Message message = Message.builder()
                                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                                .setTitle(savedNotification.getTitle())
                                                .setBody(savedNotification.getMessage())
                                                .build())
                                        .setToken(device.getFcmToken())
                                        .build();
                                try {
                                    String response = firebaseMessaging.send(message);
                                    log.info("Successfully sent message to device {}: {}", device.getFcmToken(), response);
                                } catch (FirebaseMessagingException e) {
                                    log.error("Failed to send message to device {}", device.getFcmToken(), e);
                                }
                                return true;
                            }).subscribeOn(Schedulers.boundedElastic()));
                    })
                    .then(Mono.just(savedNotification))
            );
    }

    @Override
    public Flux<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Override
    public Mono<Notification> getNotificationById(String id) {
        return notificationRepository.findById(id);
    }

    @Override
    public Mono<Page<Notification>> getNotificationsByUserId(Pageable pageable, UUID userId) {
        return notificationRepository.findAllByUserId(pageable, userId);
    }

    @Override
    public Flux<Notification> getNotificationsByUserIdAndPriority(UUID userId, NotificationPriority priority) {
        return notificationRepository.findByUserIdAndPriority(userId, priority);
    }

    @Override
    public Mono<Void> deleteNotification(String id) {
        log.info("PUSH: deleteNotification called (not implemented)");
        return Mono.empty();
    }

    @Override
    public Mono<Void> sendUserCreationNotification(UUID userId, String email, String password) {
        log.info("PUSH: sendUserCreationNotification called (not implemented)");
        return Mono.empty();
    }

    @Override
    public Mono<Void> sendPasswordResetRequestNotification(UUID userId, String email) {
        log.info("PUSH: sendPasswordResetRequestNotification called (not implemented)");
        return Mono.empty();
    }

    @Override
    public Mono<Void> sendPasswordResetApprovalNotification(UUID userId, String email) {
        log.info("PUSH: sendPasswordResetApprovalNotification called (not implemented)");
        return Mono.empty();
    }

    @Override
    public Mono<Void> sendPasswordChangeNotification(UUID userId, String email) {
        log.info("PUSH: sendPasswordChangeNotification called (not implemented)");
        return Mono.empty();
    }

    @Override
    public Mono<Void> sendPasswordChangeRejectionNotification(UUID userId, String email) {
        log.info("PUSH: sendPasswordChangeRejectionNotification called (not implemented)");
        return Mono.empty();
    }

    @Override
    public Mono<Void> broadcastNotification(String title, String message, NotificationPriority priority) {
        log.info("Broadcasting push notification: {}", title);
        return userService.getAllUsers()
            .collectList()
            .doOnNext(users -> log.info("[BROADCAST] Full user list received: {}", users))
            .flatMapMany(users -> Flux.fromIterable(users)
                .filter(user -> user.getId() != null)
                .doOnNext(user -> log.info("[BROADCAST] Iterating user: {}", user.getId()))
                .flatMap(user -> {
                    Notification notification = new Notification();
                    notification.setUserId(user.getId());
                    notification.setTitle(title);
                    notification.setMessage(message);
                    notification.setPriority(priority);
                    notification.setRead(false);
                    notification.setCreatedAt(java.time.LocalDateTime.now());
                    log.info("[BROADCAST] Creating notification for user: {}", user.getId());
                    return notificationRepository.save(notification)
                        .thenMany(userService.getUserDevices(user.getId())
                            .collectList()
                            .doOnNext(devices -> log.info("[BROADCAST] Devices for user {}: {}", user.getId(), devices))
                            .flatMapMany(devices -> {
                                if (devices.isEmpty()) {
                                    log.warn("[BROADCAST] No devices found for user {}", user.getId());
                                    return Flux.empty();
                                }
                                return Flux.fromIterable(devices)
                                    .flatMap(device -> Mono.fromCallable(() -> {
                                        log.info("[BROADCAST] Attempting to send to device: {}", device.getFcmToken());
                                        Message fcmMessage = Message.builder()
                                                .setNotification(com.google.firebase.messaging.Notification.builder()
                                                        .setTitle(title)
                                                        .setBody(message)
                                                        .build())
                                                .setToken(device.getFcmToken())
                                                .build();
                                        try {
                                            String response = firebaseMessaging.send(fcmMessage);
                                            log.info("Successfully sent broadcast message to device {}: {}", device.getFcmToken(), response);
                                        } catch (FirebaseMessagingException e) {
                                            log.error("Failed to send broadcast message to device {}", device.getFcmToken(), e);
                                        }
                                        return true;
                                    }).subscribeOn(Schedulers.boundedElastic()));
                            })
                        )
                        .doOnError(e -> log.error("[BROADCAST] Error in notification chain for user {}: {}", user.getId(), e.getMessage(), e));
                })
            )
            .doOnError(e -> log.error("[BROADCAST] Error in broadcastNotification: {}", e.getMessage(), e))
            .then();
    }

    @Override
    public Mono<Void> sendAccountDeletionNotification(UUID userId, String email) {
        log.info("PUSH: sendAccountDeletionNotification called (not implemented)");
        return Mono.empty();
    }

    @Override
    public Mono<Void> markNotificationAsRead(String id) {
        log.info("PUSH: markNotificationAsRead called (not implemented)");
        return Mono.empty();
    }

    @Override
    public Mono<Page<Notification>> getAllNotifications(Pageable pageable) {
        return notificationRepository.findAllBy(pageable);
    }
}