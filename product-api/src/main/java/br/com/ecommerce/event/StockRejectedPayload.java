package br.com.ecommerce.event;

public record StockRejectedPayload(
        Long orderId,
        Long userId,
        String reason
) {
}