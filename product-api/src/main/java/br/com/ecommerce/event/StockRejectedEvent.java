package br.com.ecommerce.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockRejectedEvent(
        UUID eventId,
        String correlationId,
        String eventType,
        String sourceService,
        LocalDateTime occurredAt,
        StockRejectedPayload payload
) {

    public static StockRejectedEvent of(
            String correlationId,
            Long orderId,
            Long userId,
            String reason
    ) {
        return new StockRejectedEvent(
                UUID.randomUUID(),
                correlationId,
                "StockRejected",
                "product-api",
                LocalDateTime.now(),
                new StockRejectedPayload(orderId, userId, reason)
        );
    }
}