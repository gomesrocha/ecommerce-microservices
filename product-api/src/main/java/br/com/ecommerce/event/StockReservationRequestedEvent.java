package br.com.ecommerce.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockReservationRequestedEvent(
        UUID eventId,
        String eventType,
        String sourceService,
        LocalDateTime occurredAt,
        StockReservationRequestedPayload payload
) {
}