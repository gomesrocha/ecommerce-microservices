package br.com.ecommerce.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", schema = "notifications")
public class Notification extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "event_id", length = 100)
    public String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    public String eventType;

    @Column(name = "aggregate_type", length = 100)
    public String aggregateType;

    @Column(name = "aggregate_id")
    public Long aggregateId;

    @Column(name = "user_id")
    public Long userId;

    @Column(nullable = false, length = 255)
    public String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public NotificationSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public NotificationStatus status;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    public String metadataJson;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "read_at")
    public LocalDateTime readAt;
}