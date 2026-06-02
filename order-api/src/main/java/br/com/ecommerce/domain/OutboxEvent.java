package br.com.ecommerce.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", schema = "orders")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "event_id", nullable = false)
    public UUID eventId;

    @Column(name = "correlation_id", length = 100)
    public String correlationId;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    public String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    public Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 120)
    public String eventType;

    @Column(name = "routing_key", nullable = false, length = 120)
    public String routingKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public OutboxStatus status;

    @Column(nullable = false)
    public Integer attempts;

    @Column(name = "last_error", length = 1000)
    public String lastError;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @Column(name = "published_at")
    public LocalDateTime publishedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.eventId == null) {
            this.eventId = UUID.randomUUID();
        }

        if (this.status == null) {
            this.status = OutboxStatus.PENDING;
        }

        if (this.attempts == null) {
            this.attempts = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}