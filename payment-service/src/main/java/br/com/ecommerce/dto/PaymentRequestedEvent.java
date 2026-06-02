package br.com.ecommerce.dto;

import java.math.BigDecimal;

public record PaymentRequestedEvent(
        String eventId,
        String correlationId,
        Long orderId,
        Long userId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String paymentToken,
        Integer installments
) {
}