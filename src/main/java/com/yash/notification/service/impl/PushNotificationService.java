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
    public Notification createNotification(Notification notification) {
        log.info("Creating push notification for user {}", notification.getUserId());
        Notification savedNotification = notificationRepository.save(notification);

        List<UserDeviceDto> userDevices = userService.getUserDevices(notification.getUserId());

        if(userDevices.isEmpty()) {
            log.warn("No devices found for user {}. Cannot send push notification.", notification.getUserId());
            return savedNotification;
        }

        for (UserDeviceDto device : userDevices) {
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
                // Here you might want to handle invalid tokens, e.g., by deleting them from the database
            }
        }

        return savedNotification;
    }

    @Override
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Override
    public Optional<Notification> getNotificationById(String id) {
        return notificationRepository.findById(id);
    }

    @Override
    public List<Notification> getNotificationsByUserId(UUID userId) {
        return notificationRepository.findByUserId(userId);
    }

    @Override
    public List<Notification> getNotificationsByUserIdAndPriority(UUID userId, NotificationPriority priority) {
        return Collections.emptyList();
    }

    @Override
    public void deleteNotification(String id) {
        log.info("PUSH: deleteNotification called (not implemented)");
    }

    @Override
    public void sendUserCreationNotification(UUID userId, String email, String password) {
        log.info("PUSH: sendUserCreationNotification called (not implemented)");
    }

    @Override
    public void sendPasswordResetRequestNotification(UUID userId, String email) {
        log.info("PUSH: sendPasswordResetRequestNotification called (not implemented)");
    }

    @Override
    public void sendPasswordResetApprovalNotification(UUID userId, String email) {
        log.info("PUSH: sendPasswordResetApprovalNotification called (not implemented)");
    }

    @Override
    public void sendPasswordChangeNotification(UUID userId, String email) {
        log.info("PUSH: sendPasswordChangeNotification called (not implemented)");
    }

    @Override
    public void sendPasswordChangeRejectionNotification(UUID userId, String email) {
        log.info("PUSH: sendPasswordChangeRejectionNotification called (not implemented)");
    }

    @Override
    public void broadcastNotification(String title, String message, NotificationPriority priority) {
        log.info("Broadcasting push notification: {}", title);
        try {
            List<UserDto> users = userService.getAllUsers();
            for (UserDto user : users) {
                Notification notification = new Notification();
                notification.setUserId(user.getId());
                notification.setTitle(title);
                notification.setMessage(message);
                notification.setPriority(priority);
                notification.setRead(false);
                notification.setCreatedAt(java.time.LocalDateTime.now());
                notificationRepository.save(notification);

                List<UserDeviceDto> userDevices = userService.getUserDevices(user.getId());
                for (UserDeviceDto device : userDevices) {
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
                }
            }
        } catch (Exception e) {
            log.error("Error in broadcastNotification", e);
            throw new RuntimeException("Failed to broadcast push notification", e);
        }
    }

    @Override
    public void sendAccountDeletionNotification(UUID userId, String email) {
        log.info("PUSH: sendAccountDeletionNotification called (not implemented)");
    }

    @Override
    public void markNotificationAsRead(String id) {
        log.info("PUSH: markNotificationAsRead called (not implemented)");
    }

    @Override
    public Page<Notification> getAllNotifications(Pageable pageable) {
        return notificationRepository.findAllBy(pageable);
    }
}