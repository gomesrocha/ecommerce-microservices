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
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static OrderResponse fromEntity(Order order) {
        return new OrderResponse(
                order.id,
                order.userId,
                order.customerState,
                order.status,
                order.totalAmount,
                order.items
                        .stream()
                        .map(OrderItemResponse::fromEntity)
                        .toList(),
                order.createdAt,
                order.updatedAt
        );
    }
}