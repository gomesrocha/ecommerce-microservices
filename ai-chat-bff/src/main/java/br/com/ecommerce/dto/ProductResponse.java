package br.com.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
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
}