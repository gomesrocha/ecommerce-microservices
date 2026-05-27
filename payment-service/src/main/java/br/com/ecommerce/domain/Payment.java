package br.com.ecommerce.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", schema = "payments")
public class Payment extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "event_id", length = 100)
    public String eventId;

    @Column(name = "order_id", nullable = false)
    public Long orderId;

    @Column(name = "user_id", nullable = false)
    public Long userId;

    @Column(nullable = false, precision = 15, scale = 2)
    public BigDecimal amount;

    @Column(nullable = false, length = 10)
    public String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    public PaymentMethod paymentMethod;

    @Column(name = "payment_token", length = 255)
    public String paymentToken;

    @Column(nullable = false)
    public Integer installments = 1;

    @Column(length = 100)
    public String provider;

    @Column(name = "provider_transaction_id", length = 100)
    public String providerTransactionId;

    @Column(name = "authorization_code", length = 100)
    public String authorizationCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    public PaymentStatus status;

    @Column(columnDefinition = "TEXT")
    public String reason;

    @Column(nullable = false)
    public Integer attempts = 0;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @Column(name = "approved_at")
    public LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    public LocalDateTime rejectedAt;
}