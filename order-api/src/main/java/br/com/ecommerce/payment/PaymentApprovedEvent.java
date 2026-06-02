package br.com.ecommerce.payment;

import java.math.BigDecimal;

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
}