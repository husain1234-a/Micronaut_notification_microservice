package com.yash.notification.repository;

import com.yash.notification.model.Notification;
import com.yash.notification.model.NotificationPriority;
import jakarta.inject.Singleton;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class NotificationRepository {
    private final DynamoDbTable<Notification> notificationTable;

    public NotificationRepository(DynamoDbEnhancedClient enhancedClient) {
        this.notificationTable = enhancedClient.table("notifications", TableSchema.fromBean(Notification.class));
    }

    public Notification save(Notification notification) {
        notificationTable.putItem(notification);
        return notification;
    }

    public Optional<Notification> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return Optional.ofNullable(notificationTable.getItem(key));
    }

    public List<Notification> findByUserId(UUID userId) {
        return notificationTable.scan()
                .items()
                .stream()
                .filter(notification -> notification.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public List<Notification> findByUserIdAndPriority(UUID userId, NotificationPriority priority) {
        return notificationTable.scan()
                .items()
                .stream()
                .filter(notification -> notification.getUserId().equals(userId) &&
                        notification.getPriority() == priority)
                .collect(Collectors.toList());
    }

    public void delete(Notification notification) {
        Key key = Key.builder().partitionValue(notification.getId()).build();
        notificationTable.deleteItem(key);
    }

    public List<Notification> findAll() {
        return notificationTable.scan()
                .items()
                .stream()
                .collect(Collectors.toList());
    }

    public List<Notification> findByPriority(NotificationPriority priority) {
        return notificationTable.scan()
                .items()
                .stream()
                .filter(notification -> notification.getPriority() == priority)
                .collect(Collectors.toList());
    }
}