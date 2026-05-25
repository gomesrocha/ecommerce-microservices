package br.com.ecommerce.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "stock_reservations",
        schema = "products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stock_reservations_order_product",
                        columnNames = {"order_id", "product_id"}
                )
        }
)
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "order_id", nullable = false)
    public Long orderId;

    @Column(name = "product_id", nullable = false)
    public Long productId;

    @Column(nullable = false)
    public Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public StockReservationStatus status;

    @Column(length = 500)
    public String reason;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}