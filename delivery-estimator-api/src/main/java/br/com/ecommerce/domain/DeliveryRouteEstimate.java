package br.com.ecommerce.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "delivery_route_estimates",
        schema = "delivery",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_delivery_route_origin_destination",
                        columnNames = {"origin_state", "destination_state"}
                )
        }
)
public class DeliveryRouteEstimate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "origin_state", nullable = false, length = 2)
    public String originState;

    @Column(name = "destination_state", nullable = false, length = 2)
    public String destinationState;

    @Column(name = "min_days", nullable = false)
    public Integer minDays;

    @Column(name = "estimated_days", nullable = false)
    public Integer estimatedDays;

    @Column(name = "max_days", nullable = false)
    public Integer maxDays;

    @Column(name = "source", nullable = false, length = 50)
    public String source;

    @Column(name = "model_version", nullable = false, length = 80)
    public String modelVersion;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();

        if (this.source == null || this.source.isBlank()) {
            this.source = "MANUAL_BASELINE";
        }

        if (this.modelVersion == null || this.modelVersion.isBlank()) {
            this.modelVersion = "baseline-routes-v1";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}