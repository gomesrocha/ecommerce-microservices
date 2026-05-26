package br.com.ecommerce.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record FraudRejectedEvent(
        UUID eventId,
        String eventType,
        String sourceService,
        LocalDateTime occurredAt,
        FraudRejectedPayload payload
) {
}