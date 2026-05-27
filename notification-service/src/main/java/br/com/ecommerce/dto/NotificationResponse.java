package br.com.ecommerce.dto;

import br.com.ecommerce.domain.Notification;
import br.com.ecommerce.domain.NotificationSeverity;
import br.com.ecommerce.domain.NotificationStatus;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String eventId,
        String eventType,
        String aggregateType,
        Long aggregateId,
        Long userId,
        String title,
        String message,
        NotificationSeverity severity,
        NotificationStatus status,
        String metadataJson,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public static NotificationResponse fromEntity(Notification notification) {
        return new NotificationResponse(
                notification.id,
                notification.eventId,
                notification.eventType,
                notification.aggregateType,
                notification.aggregateId,
                notification.userId,
                notification.title,
                notification.message,
                notification.severity,
                notification.status,
                notification.metadataJson,
                notification.createdAt,
                notification.readAt
        );
    }
}