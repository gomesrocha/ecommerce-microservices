package br.com.ecommerce.event;

import br.com.ecommerce.domain.Order;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCanceledEvent(
        UUID eventId,
        String eventType,
        String sourceService,
        LocalDateTime occurredAt,
        OrderCanceledPayload payload
) {

    public static OrderCanceledEvent fromEntity(Order order) {
        return new OrderCanceledEvent(
                UUID.randomUUID(),
                "OrderCanceled",
                "order-api",
                LocalDateTime.now(),
                OrderCanceledPayload.fromEntity(order)
        );
    }
}