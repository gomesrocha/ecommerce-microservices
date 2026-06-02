package br.com.ecommerce.event;

import br.com.ecommerce.domain.Order;
import br.com.ecommerce.observability.CorrelationIdContext;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockReservationRequestedEvent(
        UUID eventId,
        String correlationId,
        String eventType,
        String sourceService,
        LocalDateTime occurredAt,
        StockReservationRequestedPayload payload
) {

    public static StockReservationRequestedEvent fromEntity(Order order) {
        return new StockReservationRequestedEvent(
                UUID.randomUUID(),
                CorrelationIdContext.getOrCreate(),
                "StockReservationRequested",
                "order-api",
                LocalDateTime.now(),
                StockReservationRequestedPayload.fromEntity(order)
        );
    }
}