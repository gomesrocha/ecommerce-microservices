package br.com.ecommerce.event;

import java.math.BigDecimal;

public record OrderItemEvent(
        Long productId,
        String productName,
        String productSku,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String originState
) {
}