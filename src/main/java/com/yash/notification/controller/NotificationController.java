package com.yash.notification.controller;

import com.yash.notification.model.Notification;
import com.yash.notification.model.NotificationPriority;
import com.yash.notification.service.NotificationService;
import com.yash.notification.dto.BroadcastNotificationRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.scheduling.annotation.ExecuteOn;
import com.yash.notification.dto.AIGenerateRequest;
import com.yash.notification.dto.AIGenerateResponse;
import com.yash.notification.service.GeminiService;
import jakarta.inject.Named;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import com.yash.notification.dto.CreateNotificationRequest;

import java.util.List;
import java.util.UUID;

@Controller("/api/notifications")
@Tag(name = "Notification Management")
@ExecuteOn(TaskExecutors.BLOCKING)
public class NotificationController {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService emailNotificationService;
    private final NotificationService pushNotificationService;
    private final GeminiService geminiService;

    public NotificationController(
            @Named("email") NotificationService emailNotificationService,
            @Named("push") NotificationService pushNotificationService,
            GeminiService geminiService) {
        this.emailNotificationService = emailNotificationService;
        this.pushNotificationService = pushNotificationService;
        this.geminiService = geminiService;
    }

    @Post
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Operation(summary = "Create a new notification")
    public HttpResponse<Notification> createNotification(@Body @Valid Notification notification) {
        LOG.info("Creating new notification");
        return HttpResponse.created(emailNotificationService.createNotification(notification));
    }

    @ExecuteOn(TaskExecutors.BLOCKING)
    @Post("/user-creation")
    public HttpResponse<Notification> createUserNotification(@Body @Valid CreateNotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setPriority(NotificationPriority.HIGH); // or whatever default
        return HttpResponse.created(emailNotificationService.createNotification(notification));
    } 
    
    @Get
    @Operation(summary = "Get all notifications (paginated)")
    public HttpResponse<Page<Notification>> getAllNotifications(
            @QueryValue(defaultValue = "0") int page,
            @QueryValue(defaultValue = "2") int size) {
        LOG.info("Fetching notifications page {} with size {}", page, size);
        Pageable pageable = Pageable.from(page, size);
        return HttpResponse.ok(emailNotificationService.getAllNotifications(pageable));
    }

    @Get("/{id}")
    @Operation(summary = "Get notification by ID")
    public HttpResponse<Notification> getNotificationById(@PathVariable String id) {
        LOG.info("Fetching notification with id: {}", id);
        return emailNotificationService.getNotificationById(id)
                .map(HttpResponse::ok)
                .orElse(HttpResponse.notFound());
    }

    @Get("/user/{userId}")
    @Operation(summary = "Get notifications by user ID")
    public HttpResponse<List<Notification>> getNotificationsByUserId(@PathVariable UUID userId) {
        LOG.info("Fetching notifications for user: {}", userId);
        return HttpResponse.ok(emailNotificationService.getNotificationsByUserId(userId));
    }

    // @Get("/user/{userId}/priority/{priority}")
    // @Operation(summary = "Get notifications by user ID and priority")
    // public HttpResponse<List<Notification>> getNotificationsByUserIdAndPriority(
    //         @PathVariable UUID userId,
    //         @PathVariable NotificationPriority priority) {
    //     LOG.info("Fetching {} priority notifications for user: {}", priority, userId);
    //     return HttpResponse.ok(emailNotificationService.getNotificationsByUserIdAndPriority(userId, priority));
    // }

    @Patch("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public HttpResponse<Void> markNotificationAsRead(@PathVariable String id) {
        LOG.info("Marking notification as read: {}", id);
        emailNotificationService.markNotificationAsRead(id);
        return HttpResponse.noContent();
    }

    @Delete("/{id}")
    @Operation(summary = "Delete notification")
    public HttpResponse<Void> deleteNotification(@PathVariable String id) {
        LOG.info("Deleting notification with id: {}", id);
        emailNotificationService.deleteNotification(id);
        return HttpResponse.noContent();
    }

    @Post("/ai-generate")
    @Operation(summary = "Generate message using AI")
    @ExecuteOn(TaskExecutors.BLOCKING)
    public HttpResponse<AIGenerateResponse> generateAIMessage(@Body @Valid AIGenerateRequest request) {
        LOG.info("Generating AI message with prompt: {}", request.getPrompt());
        String generatedMessage = geminiService.generateMessage(request.getPrompt());
        return HttpResponse.ok(new AIGenerateResponse(generatedMessage));
    }

    @Post("/broadcast")
    @Operation(summary = "Broadcast notification to all users")
    @ExecuteOn(TaskExecutors.BLOCKING)
    public HttpResponse<Void> broadcastNotification(@Body @Valid BroadcastNotificationRequest request) {
        if (!"push".equalsIgnoreCase(request.getChannel()) && !"email".equalsIgnoreCase(request.getChannel())) {
            return HttpResponse.badRequest();
        }
        NotificationService notificationService = "push".equalsIgnoreCase(request.getChannel())
            ? pushNotificationService
            : emailNotificationService;
        notificationService.broadcastNotification(request.getTitle(), request.getMessage(), request.getPriority());
        return HttpResponse.accepted();
    }

    @Post("/test/welcome")
    @Operation(summary = "Test welcome notification")
    public void testWelcomeNotification(@Body TestNotificationRequest request) {
        emailNotificationService.sendUserCreationNotification(
                request.getUserId(),
                request.getEmail(),
                request.getPassword());
    }

    @Post("/test/reset-request")
    @Operation(summary = "Test password reset request notification")
    public void testResetRequestNotification(@Body TestNotificationRequest request) {
        emailNotificationService.sendPasswordResetRequestNotification(
                request.getUserId(),
                request.getEmail());
    }

    @Post("/test/reset-approval")
    @Operation(summary = "Test password reset approval notification")
    public void testResetApprovalNotification(@Body TestNotificationRequest request) {
        emailNotificationService.sendPasswordResetApprovalNotification(
                request.getUserId(),
                request.getEmail());
    }

    @Post("/test/password-change")
    @Operation(summary = "Test password change notification")
    public void testPasswordChangeNotification(@Body TestNotificationRequest request) {
        emailNotificationService.sendPasswordChangeNotification(
                request.getUserId(),
                request.getEmail());
    }

    @Post("/test/broadcast")
    @Operation(summary = "Test broadcast notification")
    public void testBroadcastNotification(@Body BroadcastNotificationRequest request) {
        emailNotificationService.broadcastNotification(
                request.getTitle(),
                request.getMessage(),
                request.getPriority());
    }
}

@Serdeable
class TestNotificationRequest {
    private UUID userId;
    private String email;
    private String password;

    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}