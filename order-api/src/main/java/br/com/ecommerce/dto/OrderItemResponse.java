package br.com.ecommerce.dto;

import br.com.ecommerce.domain.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        String productSku,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String originState
) {

    public static OrderItemResponse fromEntity(OrderItem item) {
        return new OrderItemResponse(
                item.id,
                item.productId,
                item.productName,
                item.productSku,
                item.quantity,
                item.unitPrice,
                item.totalPrice,
                item.originState
        );
    }
}