package br.com.ecommerce.event;

import br.com.ecommerce.domain.OrderItem;

public record StockReservationItem(
        Long productId,
        Integer quantity
) {

    public static StockReservationItem fromEntity(OrderItem item) {
        return new StockReservationItem(
                item.productId,
                item.quantity
        );
    }
}