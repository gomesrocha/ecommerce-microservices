package br.com.ecommerce.event;

public record StockReservedPayload(
        Long orderId,
        Long userId,
        String reason
) {
}