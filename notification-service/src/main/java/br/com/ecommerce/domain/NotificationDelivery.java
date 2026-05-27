package br.com.ecommerce.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_deliveries", schema = "notifications")
public class NotificationDelivery extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    public Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public NotificationChannel channel;

    @Column(length = 255)
    public String destination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public NotificationDeliveryStatus status;

    @Column(nullable = false)
    public Integer attempts = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    public String lastError;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "sent_at")
    public LocalDateTime sentAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
}