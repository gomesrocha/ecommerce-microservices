package br.com.ecommerce.event;

import br.com.ecommerce.domain.FraudAnalysis;

import java.math.BigDecimal;

public record FraudRejectedPayload(
        Long orderId,
        Long userId,
        BigDecimal totalAmount,
        BigDecimal riskScore,
        String reason
) {

    public static FraudRejectedPayload fromEntity(FraudAnalysis analysis) {
        return new FraudRejectedPayload(
                analysis.orderId,
                analysis.userId,
                analysis.totalAmount,
                analysis.riskScore,
                analysis.reason
        );
    }
}