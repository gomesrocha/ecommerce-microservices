package br.com.ecommerce.client;

public record DeliveryEstimateClientResponse(
        String originState,
        String destinationState,
        Integer totalItems,
        Integer minDays,
        Integer estimatedDays,
        Integer maxDays,
        String source,
        String modelVersion
) {
}