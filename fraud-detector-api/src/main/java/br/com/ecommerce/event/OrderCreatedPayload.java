package br.com.ecommerce.event;

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
}