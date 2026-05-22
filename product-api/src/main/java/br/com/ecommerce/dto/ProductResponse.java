package br.com.ecommerce.dto;

import br.com.ecommerce.domain.Product;
import br.com.ecommerce.domain.ProductStatus;

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
    ProductStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
            product.id,
            product.name,
            product.description,
            product.sku,
            product.price,
            product.stockQuantity,
            product.originState,
            product.status,
            product.createdAt,
            product.updatedAt
        );
    }
}