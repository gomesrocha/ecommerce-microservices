package br.com.ecommerce.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockReservedEvent(
        UUID eventId,
        String eventType,
        String sourceService,
        LocalDateTime occurredAt,
        StockReservedPayload payload
) {

    public static StockReservedEvent of(Long orderId, Long userId, String reason) {
        return new StockReservedEvent(
                UUID.randomUUID(),
                "StockReserved",
                "product-api",
                LocalDateTime.now(),
                new StockReservedPayload(orderId, userId, reason)
        );
    }
}