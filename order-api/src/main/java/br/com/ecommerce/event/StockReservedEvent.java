package br.com.ecommerce.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockReservedEvent(
        UUID eventId,
        String correlationId,
        String eventType,
        String sourceService,
        LocalDateTime occurredAt,
        StockReservedPayload payload
) {
}