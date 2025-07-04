package com.yash.notification.service;

import com.yash.notification.model.Notification;
import com.yash.notification.model.NotificationPriority;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * NotificationService interface. Implementations should be annotated
 * with @Named (e.g., "email", "sms", "push").
 * Consumers should inject with @Named or inject a List<NotificationService> for
 * dynamic selection.
 */
public interface NotificationService {
    Mono<Notification> createNotification(Notification notification);

    Flux<Notification> getAllNotifications();

    Mono<Page<Notification>> getAllNotifications(Pageable pageable);

    Mono<Notification> getNotificationById(String id);

    Mono<Page<Notification>> getNotificationsByUserId(Pageable pageable, UUID userId);

    Flux<Notification> getNotificationsByUserIdAndPriority(UUID userId, NotificationPriority priority);

    Mono<Void> deleteNotification(String id);

    Mono<Void> sendUserCreationNotification(UUID userId, String email, String password);

    Mono<Void> sendPasswordResetRequestNotification(UUID userId, String email);

    Mono<Void> sendPasswordResetApprovalNotification(UUID userId, String email);

    Mono<Void> sendPasswordChangeNotification(UUID userId, String email);

    Mono<Void> sendPasswordChangeRejectionNotification(UUID userId, String email);

    Mono<Void> broadcastNotification(String title, String message, NotificationPriority priority);

    // void broadcastNotification(String title, String message, NotificationPriority
    // priority, boolean useAI,String aiPrompt);

    Mono<Void> sendAccountDeletionNotification(UUID userId, String email);

    Mono<Void> markNotificationAsRead(String id);
}