package br.com.ecommerce.event;

import br.com.ecommerce.domain.OrderItem;

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

    public static OrderItemEvent fromEntity(OrderItem item) {
        return new OrderItemEvent(
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