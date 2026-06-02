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
}