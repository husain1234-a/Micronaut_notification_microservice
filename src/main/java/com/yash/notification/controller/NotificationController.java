package com.yash.notification.controller;

import com.yash.notification.model.Notification;
import com.yash.notification.model.NotificationPriority;
import com.yash.notification.service.NotificationService;
import com.yash.notification.dto.BroadcastNotificationRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micronaut.serde.annotation.Serdeable;
import com.yash.notification.dto.AIGenerateRequest;
import com.yash.notification.dto.AIGenerateResponse;
import com.yash.notification.service.GeminiService;
import jakarta.inject.Named;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import com.yash.notification.dto.CreateNotificationRequest;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Controller("/api/notifications")
@Tag(name = "Notification Management")
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
    @Operation(summary = "Create a new notification")
    public Mono<HttpResponse<Notification>> createNotification(@Body @Valid Notification notification) {
        LOG.info("Creating new notification");
        return emailNotificationService.createNotification(notification)
                .map(HttpResponse::created);
    }

    @Post("/user-creation")
    public Mono<HttpResponse<Notification>> createUserNotification(@Body @Valid CreateNotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setPriority(NotificationPriority.HIGH); // or whatever default
        return emailNotificationService.createNotification(notification)
                .map(HttpResponse::created);
    }

    @Get
    @Operation(summary = "Get all notifications (paginated)")
    public Mono<HttpResponse<Page<Notification>>> getAllNotifications(
            @QueryValue(defaultValue = "0") int page,
            @QueryValue(defaultValue = "2") int size) {
        LOG.info("Fetching notifications page {} with size {}", page, size);
        Pageable pageable = Pageable.from(page, size);
        return emailNotificationService.getAllNotifications(pageable)
                .map(HttpResponse::ok);
    }

    @Get("/{id}")
    @Operation(summary = "Get notification by ID")
    public Mono<MutableHttpResponse<Notification>> getNotificationById(@PathVariable String id) {
        LOG.info("Fetching notification with id: {}", id);
        return emailNotificationService.getNotificationById(id)
                .map(HttpResponse::ok)
                .defaultIfEmpty(HttpResponse.notFound((Notification) null));
    }

    @Get("/user/{userId}")
    @Operation(summary = "Get notifications by user ID")
    public Mono<HttpResponse<Page<Notification>>> getNotificationsByUserId(
            @QueryValue(defaultValue = "0") int page,
            @QueryValue(defaultValue = "2") int size,
            @PathVariable UUID userId) {
        LOG.info("Fetching notifications for user: {}", userId);
        Pageable pageable = Pageable.from(page, size);
        return emailNotificationService.getNotificationsByUserId(pageable, userId)
                .map(HttpResponse::ok);
    }

    // Uncomment and refactor if needed
    // @Get("/user/{userId}/priority/{priority}")
    // @Operation(summary = "Get notifications by user ID and priority")
    // public Flux<Notification> getNotificationsByUserIdAndPriority(
    // @PathVariable UUID userId,
    // @PathVariable NotificationPriority priority) {
    // LOG.info("Fetching {} priority notifications for user: {}", priority,
    // userId);
    // return emailNotificationService.getNotificationsByUserIdAndPriority(userId,
    // priority);
    // }

    @Patch("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public Mono<HttpResponse<Void>> markNotificationAsRead(@PathVariable String id) {
        LOG.info("Marking notification as read: {}", id);
        return emailNotificationService.markNotificationAsRead(id)
                .thenReturn(HttpResponse.noContent());
    }

    @Delete("/{id}")
    @Operation(summary = "Delete notification")
    public Mono<HttpResponse<Void>> deleteNotification(@PathVariable String id) {
        LOG.info("Deleting notification with id: {}", id);
        return emailNotificationService.deleteNotification(id)
                .thenReturn(HttpResponse.noContent());
    }

    @Post("/ai-generate")
    @Operation(summary = "Generate message using AI")
    public Mono<HttpResponse<AIGenerateResponse>> generateAIMessage(@Body @Valid AIGenerateRequest request) {
        LOG.info("Generating AI message with prompt: {}", request.getPrompt());
        return geminiService.generateMessage(request.getPrompt())
                .map(AIGenerateResponse::new)
                .map(HttpResponse::ok);
    }

    @Post("/broadcast")
    @Operation(summary = "Broadcast notification to all users")
    public Mono<HttpResponse<Void>> broadcastNotification(@Body @Valid BroadcastNotificationRequest request) {
        if (!"push".equalsIgnoreCase(request.getChannel()) && !"email".equalsIgnoreCase(request.getChannel())) {
            return Mono.just(HttpResponse.badRequest());
        }
        NotificationService notificationService = "push".equalsIgnoreCase(request.getChannel())
                ? pushNotificationService
                : emailNotificationService;
        return notificationService
                .broadcastNotification(request.getTitle(), request.getMessage(), request.getPriority())
                .thenReturn(HttpResponse.accepted());
    }

    @Post("/test/welcome")
    @Operation(summary = "Test welcome notification")
    public Mono<Void> testWelcomeNotification(@Body TestNotificationRequest request) {
        return emailNotificationService.sendUserCreationNotification(
                request.getUserId(),
                request.getEmail(),
                request.getPassword());
    }

    @Post("/test/reset-request")
    @Operation(summary = "Test password reset request notification")
    public Mono<Void> testResetRequestNotification(@Body TestNotificationRequest request) {
        return emailNotificationService.sendPasswordResetRequestNotification(
                request.getUserId(),
                request.getEmail());
    }

    @Post("/test/reset-approval")
    @Operation(summary = "Test password reset approval notification")
    public Mono<Void> testResetApprovalNotification(@Body TestNotificationRequest request) {
        return emailNotificationService.sendPasswordResetApprovalNotification(
                request.getUserId(),
                request.getEmail());
    }

    @Post("/test/password-change")
    @Operation(summary = "Test password change notification")
    public Mono<Void> testPasswordChangeNotification(@Body TestNotificationRequest request) {
        return emailNotificationService.sendPasswordChangeNotification(
                request.getUserId(),
                request.getEmail());
    }

    @Post("/test/broadcast")
    @Operation(summary = "Test broadcast notification")
    public Mono<Void> testBroadcastNotification(@Body BroadcastNotificationRequest request) {
        return emailNotificationService.broadcastNotification(
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