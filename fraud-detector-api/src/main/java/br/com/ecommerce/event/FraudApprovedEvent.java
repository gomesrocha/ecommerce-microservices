package br.com.ecommerce.event;

import br.com.ecommerce.domain.FraudAnalysis;

import java.time.LocalDateTime;
import java.util.UUID;

public record FraudApprovedEvent(
        UUID eventId,
        String correlationId,
        String eventType,
        String sourceService,
        LocalDateTime occurredAt,
        FraudApprovedPayload payload
) {

    public static FraudApprovedEvent fromEntity(String correlationId, FraudAnalysis analysis) {
        return new FraudApprovedEvent(
                UUID.randomUUID(),
                correlationId,
                "FraudApproved",
                "fraud-detector-api",
                LocalDateTime.now(),
                FraudApprovedPayload.fromEntity(analysis)
        );
    }
}