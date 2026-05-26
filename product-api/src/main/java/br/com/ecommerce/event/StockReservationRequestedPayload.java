package br.com.ecommerce.event;

import java.util.List;

public record StockReservationRequestedPayload(
        Long orderId,
        Long userId,
        List<StockReservationItem> items
) {
}