package br.com.ecommerce.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentApprovedEvent(
        String eventId,
        Long orderId,
        Long userId,
        BigDecimal amount,
        String status,
        String transactionId,
        String authorizationCode,
        String reason
) {
    public static PaymentApprovedEvent from(
            Long orderId,
            Long userId,
            BigDecimal amount,
            String transactionId,
            String authorizationCode,
            String reason
    ) {
        return new PaymentApprovedEvent(
                UUID.randomUUID().toString(),
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