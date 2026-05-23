package br.com.ecommerce.event;

import br.com.ecommerce.domain.Order;

import java.util.List;

public record StockReservationRequestedPayload(
        Long orderId,
        Long userId,
        List<StockReservationItem> items
) {

    public static StockReservationRequestedPayload fromEntity(Order order) {
        return new StockReservationRequestedPayload(
                order.id,
                order.userId,
                order.items
                        .stream()
                        .map(StockReservationItem::fromEntity)
                        .toList()
        );
    }
}