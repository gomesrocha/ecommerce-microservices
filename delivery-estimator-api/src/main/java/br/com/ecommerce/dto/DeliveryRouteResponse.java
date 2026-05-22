package br.com.ecommerce.dto;

import br.com.ecommerce.domain.DeliveryRouteEstimate;

import java.time.LocalDateTime;

public record DeliveryRouteResponse(
        Long id,
        String originState,
        String destinationState,
        Integer minDays,
        Integer estimatedDays,
        Integer maxDays,
        String source,
        String modelVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static DeliveryRouteResponse fromEntity(DeliveryRouteEstimate route) {
        return new DeliveryRouteResponse(
                route.id,
                route.originState,
                route.destinationState,
                route.minDays,
                route.estimatedDays,
                route.maxDays,
                route.source,
                route.modelVersion,
                route.createdAt,
                route.updatedAt
        );
    }
}