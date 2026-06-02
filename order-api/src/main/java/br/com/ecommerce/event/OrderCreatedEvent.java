package br.com.ecommerce.event;

import br.com.ecommerce.domain.Order;
import br.com.ecommerce.observability.CorrelationIdContext;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID eventId,
        String correlationId,
        String eventType,
        String sourceService,
        LocalDateTime occurredAt,
        OrderCreatedPayload payload
) {

    public static OrderCreatedEvent fromEntity(Order order) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                CorrelationIdContext.getOrCreate(),
                "OrderCreated",
                "order-api",
                LocalDateTime.now(),
                OrderCreatedPayload.fromEntity(order)
        );
    }
}