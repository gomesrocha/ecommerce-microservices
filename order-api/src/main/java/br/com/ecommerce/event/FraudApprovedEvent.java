package br.com.ecommerce.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record FraudApprovedEvent(
        UUID eventId,
        String eventType,
        String sourceService,
        LocalDateTime occurredAt,
        FraudApprovedPayload payload
) {
}