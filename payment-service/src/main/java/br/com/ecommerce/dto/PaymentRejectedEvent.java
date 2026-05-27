package br.com.ecommerce.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRejectedEvent(
        String eventId,
        Long orderId,
        Long userId,
        BigDecimal amount,
        String status,
        String transactionId,
        String reason
) {
    public static PaymentRejectedEvent from(
            Long orderId,
            Long userId,
            BigDecimal amount,
            String transactionId,
            String reason
    ) {
        return new PaymentRejectedEvent(
                UUID.randomUUID().toString(),
                orderId,
                userId,
                amount,
                "REJECTED",
                transactionId,
                reason
        );
    }
}