package br.com.ecommerce.event;

import br.com.ecommerce.domain.FraudAnalysis;

import java.time.LocalDateTime;
import java.util.UUID;

public record FraudRejectedEvent(
        UUID eventId,
        String eventType,
        String sourceService,
        LocalDateTime occurredAt,
        FraudRejectedPayload payload
) {

    public static FraudRejectedEvent fromEntity(FraudAnalysis analysis) {
        return new FraudRejectedEvent(
                UUID.randomUUID(),
                "FraudRejected",
                "fraud-detector-api",
                LocalDateTime.now(),
                FraudRejectedPayload.fromEntity(analysis)
        );
    }
}