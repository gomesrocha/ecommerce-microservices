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
}