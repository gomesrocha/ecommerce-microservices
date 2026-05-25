package br.com.ecommerce.dto;

import br.com.ecommerce.domain.OrderStatus;
import br.com.ecommerce.domain.OrderStatusChangeTrigger;
import br.com.ecommerce.domain.OrderStatusHistory;

import java.time.LocalDateTime;

public record OrderStatusHistoryResponse(
        Long id,
        Long orderId,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        OrderStatusChangeTrigger triggerEvent,
        String reason,
        LocalDateTime createdAt
) {

    public static OrderStatusHistoryResponse fromEntity(OrderStatusHistory history) {
        return new OrderStatusHistoryResponse(
                history.id,
                history.orderId,
                history.previousStatus,
                history.newStatus,
                history.triggerEvent,
                history.reason,
                history.createdAt
        );
    }
}