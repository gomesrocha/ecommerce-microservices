package br.com.ecommerce.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "fraud_analyses",
        schema = "fraud",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fraud_analysis_order_id", columnNames = "order_id")
        }
)
public class FraudAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "event_id", nullable = false)
    public UUID eventId;

    @Column(name = "order_id", nullable = false)
    public Long orderId;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(name = "customer_state", nullable = false, length = 2)
    public String customerState;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    public BigDecimal totalAmount;

    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    public BigDecimal riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public FraudStatus status;

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