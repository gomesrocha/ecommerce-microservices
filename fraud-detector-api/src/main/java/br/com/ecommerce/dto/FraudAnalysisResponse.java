package br.com.ecommerce.dto;

import br.com.ecommerce.domain.FraudAnalysis;
import br.com.ecommerce.domain.FraudStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record FraudAnalysisResponse(
        Long id,
        UUID eventId,
        Long orderId,
        Long userId,
        String customerState,
        BigDecimal totalAmount,
        BigDecimal riskScore,
        FraudStatus status,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static FraudAnalysisResponse fromEntity(FraudAnalysis analysis) {
        return new FraudAnalysisResponse(
                analysis.id,
                analysis.eventId,
                analysis.orderId,
                analysis.userId,
                analysis.customerState,
                analysis.totalAmount,
                analysis.riskScore,
                analysis.status,
                analysis.reason,
                analysis.createdAt,
                analysis.updatedAt
        );
    }
}