package br.com.ecommerce.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders", schema = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(name = "customer_state", nullable = false, length = 2)
    public String customerState;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    public BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @Column(name = "min_delivery_days")
    public Integer minDeliveryDays;

    @Column(name = "estimated_delivery_days")
    public Integer estimatedDeliveryDays;

    @Column(name = "max_delivery_days")
    public Integer maxDeliveryDays;

    @Column(name = "delivery_source", length = 50)
    public String deliverySource;

    @Column(name = "delivery_model_version", length = 80)
    public String deliveryModelVersion;

    @Column(name = "fraud_risk_score", precision = 5, scale = 2)
    public BigDecimal fraudRiskScore;

    @Column(name = "fraud_reason", length = 500)
    public String fraudReason;

    @Column(name = "stock_reason", length = 500)
    public String stockReason;

    @Column(name = "payment_status", length = 50)
    public String paymentStatus;

    @Column(name = "payment_transaction_id", length = 100)
    public String paymentTransactionId;

    @Column(name = "payment_authorization_code", length = 100)
    public String paymentAuthorizationCode;

    @Column(name = "payment_reason", columnDefinition = "TEXT")
    public String paymentReason;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    public List<OrderItem> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.status == null) {
            this.status = OrderStatus.CREATED;
        }

        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addItem(OrderItem item) {
        item.order = this;
        this.items.add(item);
    }

    public boolean canBeCanceled() {
    return OrderStatus.CREATED.equals(this.status)
            || OrderStatus.WAITING_STOCK.equals(this.status)
            || OrderStatus.WAITING_FRAUD.equals(this.status)
            || OrderStatus.CONFIRMED.equals(this.status);
    }
}