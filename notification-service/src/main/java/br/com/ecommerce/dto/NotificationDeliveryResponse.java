package br.com.ecommerce.dto;

import br.com.ecommerce.domain.NotificationChannel;
import br.com.ecommerce.domain.NotificationDelivery;
import br.com.ecommerce.domain.NotificationDeliveryStatus;

import java.time.LocalDateTime;

public record NotificationDeliveryResponse(
        Long id,
        Long notificationId,
        NotificationChannel channel,
        String destination,
        NotificationDeliveryStatus status,
        Integer attempts,
        String lastError,
        LocalDateTime createdAt,
        LocalDateTime sentAt,
        LocalDateTime updatedAt
) {
    public static NotificationDeliveryResponse fromEntity(NotificationDelivery delivery) {
        return new NotificationDeliveryResponse(
                delivery.id,
                delivery.notification.id,
                delivery.channel,
                delivery.destination,
                delivery.status,
                delivery.attempts,
                delivery.lastError,
                delivery.createdAt,
                delivery.sentAt,
                delivery.updatedAt
        );
    }
}