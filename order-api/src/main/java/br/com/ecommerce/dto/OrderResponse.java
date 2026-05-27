package br.com.ecommerce.dto;

import br.com.ecommerce.domain.Order;
import br.com.ecommerce.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        String customerState,
        OrderStatus status,
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
        LocalDateTime updatedAt,
        String paymentStatus,
        String paymentTransactionId,
        String paymentAuthorizationCode,
        String paymentReason
) {

    public static OrderResponse fromEntity(Order order) {
        return new OrderResponse(
                order.id,
                order.userId,
                order.customerState,
                order.status,
                order.totalAmount,
                order.minDeliveryDays,
                order.estimatedDeliveryDays,
                order.maxDeliveryDays,
                order.deliverySource,
                order.deliveryModelVersion,
                order.fraudRiskScore,
                order.fraudReason,
                order.stockReason,
                order.items
                        .stream()
                        .map(OrderItemResponse::fromEntity)
                        .toList(),
                order.createdAt,
                order.updatedAt,
                order.paymentStatus,
                order.paymentTransactionId,
                order.paymentAuthorizationCode,
                order.paymentReason
        );
    }
}