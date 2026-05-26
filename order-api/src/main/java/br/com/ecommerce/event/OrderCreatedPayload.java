package br.com.ecommerce.event;

import br.com.ecommerce.domain.Order;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedPayload(
        Long orderId,
        Long userId,
        String customerState,
        String status,
        BigDecimal totalAmount,
        Integer minDeliveryDays,
        Integer estimatedDeliveryDays,
        Integer maxDeliveryDays,
        String deliverySource,
        String deliveryModelVersion,
        List<OrderItemEvent> items
) {

    public static OrderCreatedPayload fromEntity(Order order) {
        return new OrderCreatedPayload(
                order.id,
                order.userId,
                order.customerState,
                order.status.name(),
                order.totalAmount,
                order.minDeliveryDays,
                order.estimatedDeliveryDays,
                order.maxDeliveryDays,
                order.deliverySource,
                order.deliveryModelVersion,
                order.items
                        .stream()
                        .map(OrderItemEvent::fromEntity)
                        .toList()
        );
    }
}