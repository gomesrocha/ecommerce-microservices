package br.com.ecommerce.event;

import java.math.BigDecimal;

public record FraudApprovedPayload(
        Long orderId,
        Long userId,
        BigDecimal totalAmount,
        BigDecimal riskScore,
        String reason
) {
}