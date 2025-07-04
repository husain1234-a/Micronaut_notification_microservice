package com.yash.notification.repository;

import com.yash.notification.model.Notification;
import com.yash.notification.model.NotificationPriority;

import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Singleton;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Singleton
public class NotificationRepository {
    private final DynamoDbTable<Notification> notificationTable;

    public NotificationRepository(DynamoDbEnhancedClient enhancedClient) {
        this.notificationTable = enhancedClient.table("notifications", TableSchema.fromBean(Notification.class));
    }

    public Mono<Notification> save(Notification notification) {
        return Mono.fromCallable(() -> {
            notificationTable.putItem(notification);
            return notification;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Notification> findById(String id) {
        return Mono.fromCallable(() -> {
            Key key = Key.builder().partitionValue(id).build();
            return notificationTable.getItem(key);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<Notification> findByUserIdAndPriority(UUID userId, NotificationPriority priority) {
        return Flux.defer(() -> Flux.fromStream(
            notificationTable.scan()
                .items()
                .stream()
               .filter(notification -> notification.getUserId() != null && notification.getUserId().equals(userId) &&
                        notification.getPriority() == priority)
        )).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> delete(Notification notification) {
        return Mono.fromRunnable(() -> {
            Key key = Key.builder().partitionValue(notification.getId()).build();
            notificationTable.deleteItem(key);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Flux<Notification> findAll() {
        return Flux.defer(() -> Flux.fromStream(
            notificationTable.scan()
                .items()
                .stream()
        )).subscribeOn(Schedulers.boundedElastic());
    }

    public Flux<Notification> findByPriority(NotificationPriority priority) {
        return Flux.defer(() -> Flux.fromStream(
            notificationTable.scan()
                .items()
                .stream()
                .filter(notification -> notification.getPriority() == priority)
        )).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Page<Notification>> findAllBy(Pageable pageable) {
        return findAll().collectList().map(allNotifications -> {
            int total = allNotifications.size();
            int pageNumber = pageable.getNumber();
            int pageSize = pageable.getSize();
            int fromIndex = pageNumber * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);

            List<Notification> pageContent;
            if (fromIndex >= total) {
                pageContent = Collections.emptyList();
            } else {
                pageContent = allNotifications.subList(fromIndex, toIndex);
            }

            return Page.of(pageContent, pageable, (long) total);
        });
    }

    public Mono<Page<Notification>> findAllByUserId(Pageable pageable, UUID userId) {
        return findAll().filter(notification -> notification.getUserId() != null && notification.getUserId().equals(userId)).collectList().map(allNotificationsByUserId -> {
            int total = allNotificationsByUserId.size();
            int pageNumber = pageable.getNumber();
            int pageSize = pageable.getSize();
            int fromIndex = pageNumber * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);

            List<Notification> pageContent;
            if (fromIndex >= total) {
                pageContent = Collections.emptyList();
            } else {
                pageContent = allNotificationsByUserId.subList(fromIndex, toIndex);
            }

            return Page.of(pageContent, pageable, (long) total);
        });
    }
}