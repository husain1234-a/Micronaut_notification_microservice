package com.yash.notification.service;

import com.yash.notification.model.Notification;
import com.yash.notification.model.NotificationPriority;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * NotificationService interface. Implementations should be annotated
 * with @Named (e.g., "email", "sms", "push").
 * Consumers should inject with @Named or inject a List<NotificationService> for
 * dynamic selection.
 */
public interface NotificationService {
    Notification createNotification(Notification notification);

    List<Notification> getAllNotifications();

    Optional<Notification> getNotificationById(String id);

    List<Notification> getNotificationsByUserId(UUID userId);

    List<Notification> getNotificationsByUserIdAndPriority(UUID userId, NotificationPriority priority);

    void deleteNotification(String id);

    void sendUserCreationNotification(UUID userId, String email, String password);

    void sendPasswordResetRequestNotification(UUID userId, String email);

    void sendPasswordResetApprovalNotification(UUID userId, String email);

    void sendPasswordChangeNotification(UUID userId, String email);

    void sendPasswordChangeRejectionNotification(UUID userId, String email);

    void broadcastNotification(String title, String message, NotificationPriority priority);

    // void broadcastNotification(String title, String message, NotificationPriority
    // priority, boolean useAI,String aiPrompt);

    void sendAccountDeletionNotification(UUID userId, String email);

    void markNotificationAsRead(String id);
}