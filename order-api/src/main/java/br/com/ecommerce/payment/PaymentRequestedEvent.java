package br.com.ecommerce.payment;

import br.com.ecommerce.observability.CorrelationIdContext;

import java.math.BigDecimal;
import java.util.UUID;

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
    public static PaymentRequestedEvent fromOrder(
            Long orderId,
            Long userId,
            BigDecimal amount
    ) {
        return new PaymentRequestedEvent(
                UUID.randomUUID().toString(),
                CorrelationIdContext.getOrCreate(),
                orderId,
                userId,
                amount,
                "BRL",
                "CREDIT_CARD",
                "tok_approved",
                1
        );
    }
}