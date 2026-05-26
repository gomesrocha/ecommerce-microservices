package br.com.ecommerce.event;

import java.math.BigDecimal;

public record FraudRejectedPayload(
        Long orderId,
        Long userId,
        BigDecimal totalAmount,
        BigDecimal riskScore,
        String reason
) {
}