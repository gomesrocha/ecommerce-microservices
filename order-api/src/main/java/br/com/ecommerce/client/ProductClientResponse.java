package br.com.ecommerce.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductClientResponse(
    Long id,
    String name,
    String description,
    String sku,
    BigDecimal price,
    Integer stockQuantity,
    String originState,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public boolean hasStock(Integer quantity) {
        return quantity != null
                && quantity > 0
                && stockQuantity != null
                && stockQuantity >= quantity;
    }
}