package br.com.ecommerce.dto;

import java.util.List;

public record CreateOrderRequest(
        Long userId,
        String customerState,
        List<CreateOrderItemRequest> items
) {
}
