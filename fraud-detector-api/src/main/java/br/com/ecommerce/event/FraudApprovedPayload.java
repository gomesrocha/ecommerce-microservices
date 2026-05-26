package br.com.ecommerce.event;

import br.com.ecommerce.domain.FraudAnalysis;

import java.math.BigDecimal;

public record FraudApprovedPayload(
        Long orderId,
        Long userId,
        BigDecimal totalAmount,
        BigDecimal riskScore,
        String reason
) {

    public static FraudApprovedPayload fromEntity(FraudAnalysis analysis) {
        return new FraudApprovedPayload(
                analysis.orderId,
                analysis.userId,
                analysis.totalAmount,
                analysis.riskScore,
                analysis.reason
        );
    }
}