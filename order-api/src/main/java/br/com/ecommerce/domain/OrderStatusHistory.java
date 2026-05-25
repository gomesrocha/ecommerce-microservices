package br.com.ecommerce.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history", schema = "orders")
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "order_id", nullable = false)
    public Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    public OrderStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    public OrderStatus newStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_event", nullable = false, length = 80)
    public OrderStatusChangeTrigger triggerEvent;

    @Column(length = 500)
    public String reason;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}