package br.com.ecommerce.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentApprovedEvent(
        String eventId,
        String correlationId,
        Long orderId,
        Long userId,
        BigDecimal amount,
        String status,
        String transactionId,
        String authorizationCode,
        String reason
) {
    public static PaymentApprovedEvent from(
            String correlationId,
            Long orderId,
            Long userId,
            BigDecimal amount,
            String transactionId,
            String authorizationCode,
            String reason
    ) {
        return new PaymentApprovedEvent(
                UUID.randomUUID().toString(),
                correlationId,
                orderId,
                userId,
                amount,
                "APPROVED",
                transactionId,
                authorizationCode,
                reason
        );
    }
}