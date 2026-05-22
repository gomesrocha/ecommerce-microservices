package br.com.ecommerce.domain;


import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "products",
    schema = "products",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_products_sku", columnNames = "sku")
    }
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 120)
    public String name;

    @Column(length = 500)
    public String description;

    @Column(nullable = false, length = 80)
    public String sku;

    @Column(nullable = false, precision = 10, scale = 2)
    public BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    public Integer stockQuantity;

    @Column(name = "origin_state", nullable = false, length = 2)
    public String originState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public ProductStatus status;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = ProductStatus.ACTIVE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return ProductStatus.ACTIVE.equals(this.status);
    }

    public boolean hasStock(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return false;
        }

        return this.stockQuantity != null && this.stockQuantity >= quantity;
    }
}