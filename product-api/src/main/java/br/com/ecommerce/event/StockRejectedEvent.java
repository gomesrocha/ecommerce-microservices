package br.com.ecommerce.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockRejectedEvent(
        UUID eventId,
        String eventType,
        String sourceService,
        LocalDateTime occurredAt,
        StockRejectedPayload payload
) {

    public static StockRejectedEvent of(Long orderId, Long userId, String reason) {
        return new StockRejectedEvent(
                UUID.randomUUID(),
                "StockRejected",
                "product-api",
                LocalDateTime.now(),
                new StockRejectedPayload(orderId, userId, reason)
        );
    }
}