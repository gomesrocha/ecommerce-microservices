package br.com.ecommerce.dto;

public record CreateOrderItemRequest(
        Long productId,
        Integer quantity
) {
}
