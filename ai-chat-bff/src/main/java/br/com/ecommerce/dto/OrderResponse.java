package br.com.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        String customerState,
        String status,
        BigDecimal totalAmount,
        Integer minDeliveryDays,
        Integer estimatedDeliveryDays,
        Integer maxDeliveryDays,
        String deliverySource,
        String deliveryModelVersion,
        BigDecimal fraudRiskScore,
        String fraudReason,
        String stockReason,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}