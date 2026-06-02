package br.com.ecommerce.event;

import br.com.ecommerce.domain.FraudAnalysis;

import java.time.LocalDateTime;
import java.util.UUID;

public record FraudRejectedEvent(
        UUID eventId,
        String correlationId,
        String eventType,
        String sourceService,
        LocalDateTime occurredAt,
        FraudRejectedPayload payload
) {

    public static FraudRejectedEvent fromEntity(String correlationId, FraudAnalysis analysis) {
        return new FraudRejectedEvent(
                UUID.randomUUID(),
                correlationId,
                "FraudRejected",
                "fraud-detector-api",
                LocalDateTime.now(),
                FraudRejectedPayload.fromEntity(analysis)
        );
    }
}