package br.com.ecommerce.event;

public record StockReservationItem(
        Long productId,
        Integer quantity
) {
}