package br.com.ecommerce.event;

import br.com.ecommerce.domain.Order;

import java.math.BigDecimal;

public record OrderCanceledPayload(
        Long orderId,
        Long userId,
        String customerState,
        String status,
        BigDecimal totalAmount
) {

    public static OrderCanceledPayload fromEntity(Order order) {
        return new OrderCanceledPayload(
                order.id,
                order.userId,
                order.customerState,
                order.status.name(),
                order.totalAmount
        );
    }
}