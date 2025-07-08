package com.yash.notification.service.impl;

import com.yash.notification.exception.ResourceNotFoundException;
import com.yash.notification.model.Notification;
import com.yash.notification.model.NotificationPriority;
import com.yash.notification.dto.UserDto;
import com.yash.notification.repository.NotificationRepository;
import com.yash.notification.service.NotificationService;
import com.yash.notification.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.yash.notification.config.SendGridConfig;
import com.yash.notification.service.impl.SendGridEmailService;
import com.yash.notification.service.GeminiService;
import jakarta.inject.Named;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Named("email")
public class EmailNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    // private static final String ADMIN_EMAIL = "en20cs301184@medicaps.ac.in";
    private static final String RESET_PASSWORD_URL = "http://localhost/reset-password";

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final SendGridEmailService sendGridEmailService;
    private final SendGridConfig sendGridConfig;
    private final GeminiService geminiService;

    public EmailNotificationService(
            NotificationRepository notificationRepository,
            UserService userService,
            SendGridEmailService sendGridEmailService,
            SendGridConfig sendGridConfig,
            GeminiService geminiService) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
        this.sendGridEmailService = sendGridEmailService;
        this.sendGridConfig = sendGridConfig;
        this.geminiService = geminiService;
    }

    @Override
    public Mono<Notification> createNotification(Notification notification) {
        log.info("[DEBUG] Entered createNotification with userId: {}", notification.getUserId());
        notification.setId(UUID.randomUUID().toString());
        notification.setRead(false);
        notification.setCreatedAt(java.time.LocalDateTime.now());
        return notificationRepository.save(notification)
            .flatMap(saved ->
                userService.getUserById(notification.getUserId())
                    .flatMap(user -> Mono.fromCallable(() -> {
                        String subject = notification.getTitle();
                        String textContent = notification.getMessage();
                        String htmlContent = "<p>" + notification.getMessage() + "</p>";
                        boolean emailSent = sendGridEmailService.sendEmail(
                                user.getEmail(),
                                subject,
                                textContent,
                                htmlContent);
                        if (emailSent) {
                            log.info("[DEBUG] Email sent to user: {}", user.getEmail());
                        } else {
                            log.warn("[DEBUG] Failed to send email to user: {}", user.getEmail());
                        }
                        return saved;
                    }).subscribeOn(Schedulers.boundedElastic()))
                    .onErrorResume(e -> {
                        log.error("[DEBUG] Error sending email for notification to userId: {}", notification.getUserId(), e);
                        return Mono.just(saved);
                    })
            );
    }

    @Override
    public Flux<Notification> getAllNotifications() {
        log.info("Fetching all notifications");
        return notificationRepository.findAll();
    }

    @Override
    public Mono<Notification> getNotificationById(String id) {
        log.info("Fetching notification with id: {}", id);
        return notificationRepository.findById(id);
    }

    @Override
    public Mono<Page<Notification>> getNotificationsByUserId(Pageable pageable, UUID userId) {
        return userService.getUserById(userId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with id: " + userId)))
            .then(notificationRepository.findAllByUserId(pageable, userId));
    }

    @Override
    public Flux<Notification> getNotificationsByUserIdAndPriority(UUID userId, NotificationPriority priority) {
        return userService.getUserById(userId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with id: " + userId)))
            .thenMany(notificationRepository.findByUserIdAndPriority(userId, priority));
    }

    @Override
    public Mono<Void> deleteNotification(String id) {
        log.info("Deleting notification with id: {}", id);
        return notificationRepository.findById(id)
            .flatMap(notificationRepository::delete)
            .then();
    }

    @Override
    public Mono<Void> sendUserCreationNotification(UUID userId, String email, String password) {
        log.info("Sending user creation notification to: {}", email);
        return userService.getUserById(userId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with id: " + userId)))
            .flatMap(user -> {
                Notification notification = new Notification();
                notification.setUserId(userId);
                notification.setTitle("Welcome to User Management System");
                notification.setMessage("Your account has been created successfully.");
                notification.setPriority(NotificationPriority.HIGH);
                notification.setRead(false);
                notification.setCreatedAt(java.time.LocalDateTime.now());
                return notificationRepository.save(notification)
                    .then(Mono.fromCallable(() -> {
                        String plainTextBody = "Welcome to User Management System!\n\n" +
                                "Your account has been created successfully.\n" +
                                "Your temporary password is: " + password + "\n\n" +
                                "Please change your password after first login.";
                        String htmlBody = "<h2>Welcome to User Management System</h2><br>" +
                                "<p>Your account has been created successfully.</p>" +
                                "<p>Your temporary password is: <strong>" + password + "</strong></p>" +
                                "<p>Please change your password after first login.</p>";
                        boolean emailSent = sendGridEmailService.sendEmail(
                                user.getEmail(),
                                "Welcome to User Management System",
                                plainTextBody,
                                htmlBody);
                        if (!emailSent) {
                            log.warn("Failed to send welcome email to user: {}", user.getEmail());
                        }
                        return true;
                    }).subscribeOn(Schedulers.boundedElastic()));
            })
            .then();
    }

    @Override
    public Mono<Void> sendPasswordResetRequestNotification(UUID userId, String email) {
        log.info("Sending password reset request notification for user: {}", userId);
        return userService.getUserById(userId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with id: " + userId)))
            .flatMap(user -> {
                Notification notification = new Notification();
                notification.setUserId(userId);
                notification.setTitle("Password Reset Request");
                notification.setMessage("A password reset has been requested for your account.");
                notification.setPriority(NotificationPriority.HIGH);
                notification.setRead(false);
                notification.setCreatedAt(java.time.LocalDateTime.now());
                return notificationRepository.save(notification)
                    .thenMany(userService.getUsersByRole("ADMIN"))
                    .flatMap(admin -> {
                        Notification adminNotification = new Notification();
                        adminNotification.setUserId(admin.getId());
                        adminNotification.setTitle("New Password Change Request");
                        adminNotification.setMessage("A new password change request has been submitted by user: "
                                + user.getFirstName() + " " + user.getLastName() + " (" + user.getEmail() + ")");
                        adminNotification.setPriority(NotificationPriority.HIGH);
                        adminNotification.setRead(false);
                        adminNotification.setCreatedAt(java.time.LocalDateTime.now());
                        return notificationRepository.save(adminNotification);
                    })
                    .then(Mono.fromCallable(() -> {
                        String subject = "Password Reset Request";
                        String textContent = "A password reset has been requested for your account.\n" +
                                "Please wait for admin approval.";
                        String htmlContent = "<h3>Password Reset Request</h3><br>" +
                                "<p>A password reset has been requested for your account.</p><br>" +
                                "<p>Please wait for admin approval.</p>";
                        sendGridEmailService.sendEmail(
                                user.getEmail(),
                                subject,
                                textContent,
                                htmlContent);
                        return true;
                    }).subscribeOn(Schedulers.boundedElastic()));
            })
            .then();
    }

    @Override
    public Mono<Void> sendPasswordResetApprovalNotification(UUID userId, String email) {
        log.info("Sending password reset approval notification for user: {}", userId);
        return Mono.empty();
    }

    @Override
    public Mono<Void> sendPasswordChangeNotification(UUID userId, String email) {
        log.info("Sending password change notification for user: {}", userId);
        return Mono.empty();
    }

    @Override
    public Mono<Void> sendPasswordChangeRejectionNotification(UUID userId, String email) {
        log.info("Sending password change rejection notification for user: {}", userId);
        return Mono.empty();
    }

    @Override
    public Mono<Void> broadcastNotification(String title, String message, NotificationPriority priority) {
        log.info("Broadcasting email notification: {}", title);
        return userService.getAllUsers()
            .flatMap(user -> {
                Notification notification = new Notification();
                notification.setUserId(user.getId());
                notification.setTitle(title);
                notification.setMessage(message);
                notification.setPriority(priority);
                notification.setRead(false);
                notification.setCreatedAt(java.time.LocalDateTime.now());
                return notificationRepository.save(notification)
                    .then(Mono.fromCallable(() -> {
                        sendGridEmailService.sendEmail(
                                user.getEmail(),
                                title,
                                message,
                                "<p>" + message + "</p>");
                        return true;
                    }).subscribeOn(Schedulers.boundedElastic()));
            })
            .then();
    }

    @Override
    public Mono<Void> sendAccountDeletionNotification(UUID userId, String email) {
        log.info("Sending account deletion notification for user: {}", userId);
        return userService.getUserById(userId)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found with id: " + userId)))
            .flatMap(user -> {
                Notification notification = new Notification();
                notification.setUserId(userId);
                notification.setTitle("Account Deleted");
                notification.setMessage("Your account has been deleted. If this was not you, please contact support.");
                notification.setPriority(NotificationPriority.HIGH);
                notification.setRead(false);
                notification.setCreatedAt(java.time.LocalDateTime.now());
                return notificationRepository.save(notification)
                    .then(Mono.fromCallable(() -> {
                        String subject = "Account Deleted";
                        String plainTextBody = "Your account has been deleted. If this was not you, please contact support.";
                        String htmlBody = "<h2>Account Deleted</h2><p>Your account has been deleted. If this was not you, please contact support.</p>";
                        boolean emailSent = sendGridEmailService.sendEmail(
                                email,
                                subject,
                                plainTextBody,
                                htmlBody);
                        if (!emailSent) {
                            log.warn("Failed to send account deletion email to user: {}", email);
                        }
                        return true;
                    }).subscribeOn(Schedulers.boundedElastic()));
            })
            .then();
    }

    @Override
    public Mono<Void> markNotificationAsRead(String id) {
        log.info("Marking notification as read: {}", id);
        return Mono.empty();
    }

    @Override
    public Mono<Page<Notification>> getAllNotifications(Pageable pageable) {
        return notificationRepository.findAllBy(pageable);
    }
}