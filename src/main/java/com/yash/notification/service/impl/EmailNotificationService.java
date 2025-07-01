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
    public Notification createNotification(Notification notification) {
        // Validate user exists
        UserDto user = userService.getUserById(notification.getUserId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("User not found with id: " + notification.getUserId()));
        notification.setId(UUID.randomUUID().toString());
        notification.setRead(false);
        notification.setCreatedAt(java.time.LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getAllNotifications() {
        log.info("Fetching all notifications");
        return notificationRepository.findAll();
    }

    @Override
    public Optional<Notification> getNotificationById(String id) {
        log.info("Fetching notification with id: {}", id);
        return notificationRepository.findById(id);
    }

    @Override
    public List<Notification> getNotificationsByUserId(UUID userId) {
        userService.getUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        log.info("Fetching notifications for user: {}", userId);
        return notificationRepository.findByUserId(userId);
    }

    @Override
    public List<Notification> getNotificationsByUserIdAndPriority(UUID userId, NotificationPriority priority) {
        userService.getUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        log.info("Fetching {} priority notifications for user: {}", priority, userId);
        return notificationRepository.findByUserIdAndPriority(userId, priority);
    }

    @Override
    public void deleteNotification(String id) {
        log.info("Deleting notification with id: {}", id);
        notificationRepository.findById(id).ifPresent(notificationRepository::delete);
    }

    @Override
    public void sendUserCreationNotification(UUID userId, String email, String password) {
        log.info("Sending user creation notification to: {}", email);
        try {
            UserDto user = userService.getUserById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setTitle("Welcome to User Management System");
            notification.setMessage("Your account has been created successfully.");
            notification.setPriority(NotificationPriority.HIGH);
            notification.setRead(false);
            notification.setCreatedAt(java.time.LocalDateTime.now());
            notificationRepository.save(notification);

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

        } catch (Exception e) {
            log.error("Error in sendUserCreationNotification for user: {}", userId, e);
            throw e;
        }
    }

    @Override
    public void sendPasswordResetRequestNotification(UUID userId, String email) {
        log.info("Sending password reset request notification for user: {}", userId);
        try {
            UserDto user = userService.getUserById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setTitle("Password Reset Request");
            notification.setMessage("A password reset has been requested for your account.");
            notification.setPriority(NotificationPriority.HIGH);
            notification.setRead(false);
            notification.setCreatedAt(java.time.LocalDateTime.now());
            notificationRepository.save(notification);

            // Send email to all admins in the system
            List<UserDto> admins = userService.getUsersByRole("ADMIN");
            for (UserDto admin : admins) {
                Notification adminNotification = new Notification();
                adminNotification.setUserId(admin.getId());
                adminNotification.setTitle("New Password Change Request");
                adminNotification.setMessage("A new password change request has been submitted by user: "
                        + user.getFirstName() + " " + user.getLastName() + " (" + user.getEmail() + ")");
                adminNotification.setPriority(NotificationPriority.HIGH);
                adminNotification.setRead(false);
                adminNotification.setCreatedAt(java.time.LocalDateTime.now());
                notificationRepository.save(adminNotification);
            }

            // Send email to user
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

            // Send email to all admins in the system
            String adminSubject = "New Password Change Request";
            String adminTextContent = "A new password change request has been submitted by user:\n" +
                    "User ID: " + userId + "\n" +
                    "User Name: " + user.getFirstName() + " " + user.getLastName() + "\n" +
                    "User Email: " + user.getEmail() + "\n\n" +
                    "Please review and take appropriate action.";
            String adminHtmlContent = "<h3>New Password Change Request</h3><br>" +
                    "<p>A new password change request has been submitted by user:</p>" +
                    "<p><strong>User ID:</strong> " + userId + "</p>" +
                    "<p><strong>User Name:</strong> " + user.getFirstName() + " " + user.getLastName() + "</p>" +
                    "<p><strong>User Email:</strong> " + user.getEmail() + "</p><br>" +
                    "<p>Please review and take appropriate action.</p>";

            for (UserDto admin : admins) {
                sendGridEmailService.sendEmail(
                        admin.getEmail(),
                        adminSubject,
                        adminTextContent,
                        adminHtmlContent);
            }

        } catch (Exception e) {
            log.error("Error in sendPasswordResetRequestNotification for user: {}", userId, e);
            throw e;
        }
    }

    @Override
    public void sendPasswordResetApprovalNotification(UUID userId, String email) {
        log.info("Sending password reset approval notification for user: {}", userId);
        try {
            UserDto user = userService.getUserById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setTitle("Password Reset Approved");
            notification.setMessage("Your password reset request has been approved.");
            notification.setPriority(NotificationPriority.HIGH);
            notification.setRead(false);
            notification.setCreatedAt(java.time.LocalDateTime.now());
            notificationRepository.save(notification);

            String subject = "Password Reset Approved";
            String textContent = "Your password reset request has been approved.\n" +
                    "You can now reset your password using the following link:\n" +
                    RESET_PASSWORD_URL;
            String htmlContent = "<h3>Password Reset Approved</h3><br>" +
                    "<p>Your password reset request has been approved.</p><br>" +
                    "<p>You can now reset your password using the following link:</p>" +
                    "<a href='" + RESET_PASSWORD_URL + "'>Reset Password</a>";

            sendGridEmailService.sendEmail(
                    user.getEmail(),
                    subject,
                    textContent,
                    htmlContent);

        } catch (Exception e) {
            log.error("Error in sendPasswordResetApprovalNotification for user: {}", userId, e);
            throw e;
        }
    }

    @Override
    public void sendPasswordChangeNotification(UUID userId, String email) {
        log.info("Sending password change notification for user: {}", userId);
        try {
            UserDto user = userService.getUserById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setTitle("Password Changed Successfully");
            notification.setMessage("Your password has been changed successfully.");
            notification.setPriority(NotificationPriority.HIGH);
            notification.setRead(false);
            notification.setCreatedAt(java.time.LocalDateTime.now());
            notificationRepository.save(notification);

            String subject = "Password Changed Successfully";
            String textContent = "Your password has been changed successfully.\n" +
                    "If you did not make this change, please contact support immediately.";
            String htmlContent = "<h3>Password Changed Successfully</h3><br>" +
                    "<p>Your password has been changed successfully.</p><br>" +
                    "<p>If you did not make this change, please contact support immediately.</p>";

            sendGridEmailService.sendEmail(
                    user.getEmail(),
                    subject,
                    textContent,
                    htmlContent);

        } catch (Exception e) {
            log.error("Error in sendPasswordChangeNotification for user: {}", userId, e);
            throw e;
        }
    }

    // @Override
    // public void broadcastNotification(String title, String message,
    // NotificationPriority priority, boolean useAI,
    // String aiPrompt) {
    // if (useAI && aiPrompt != null && !aiPrompt.isEmpty()) {
    // message = geminiService.generateMessage(aiPrompt);
    // }
    // log.info("Message Generated from AI "+ message);
    // broadcastNotification(title, message, priority);
    // }

    @Override
    public void broadcastNotification(String title, String message, NotificationPriority priority) {
        log.info("Broadcasting notification: {}", title);
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
            }
        } catch (Exception e) {
            log.error("Error in broadcastNotification", e);
            throw e;
        }
    }

    @Override
    public void sendPasswordChangeRejectionNotification(UUID userId, String email) {
        log.info("Sending password change rejection notification for user: {}", userId);
        try {
            UserDto user = userService.getUserById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setTitle("Password Reset Request Rejected");
            notification.setMessage("Your password reset request has been rejected.");
            notification.setPriority(NotificationPriority.HIGH);
            notification.setRead(false);
            notification.setCreatedAt(java.time.LocalDateTime.now());
            notificationRepository.save(notification);

            String subject = "Password Reset Request Rejected";
            String textContent = "Your password reset request has been rejected.\n" +
                    "Please contact support for more information.";
            String htmlContent = "<h3>Password Reset Request Rejected</h3><br>" +
                    "<p>Your password reset request has been rejected.</p><br>" +
                    "<p>Please contact support for more information.</p>";

            sendGridEmailService.sendEmail(
                    user.getEmail(),
                    subject,
                    textContent,
                    htmlContent);

        } catch (Exception e) {
            log.error("Error in sendPasswordChangeRejectionNotification for user: {}", userId, e);
            throw e;
        }
    }

    @Override
    public void sendAccountDeletionNotification(UUID userId, String email) {
        log.info("Sending account deletion notification for user: {}", userId);
        try {
            UserDto user = userService.getUserById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setTitle("Account Deleted");
            notification.setMessage("Your account has been deleted successfully.");
            notification.setPriority(NotificationPriority.HIGH);
            notification.setRead(false);
            notification.setCreatedAt(java.time.LocalDateTime.now());
            notificationRepository.save(notification);

            String subject = "Account Deleted";
            String textContent = "Your account has been deleted successfully.\n" +
                    "Thank you for using our service.";
            String htmlContent = "<h3>Account Deleted</h3><br>" +
                    "<p>Your account has been deleted successfully.</p><br>" +
                    "<p>Thank you for using our service.</p>";

            sendGridEmailService.sendEmail(
                    user.getEmail(),
                    subject,
                    textContent,
                    htmlContent);

        } catch (Exception e) {
            log.error("Error in sendAccountDeletionNotification for user: {}", userId, e);
            throw e;
        }
    }

    @Override
    public void markNotificationAsRead(String id) {
        log.info("Marking notification as read: {}", id);
        notificationRepository.findById(id).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    @Override
    public Page<Notification> getAllNotifications(Pageable pageable) {
        return notificationRepository.findAll(pageable);
    }
}