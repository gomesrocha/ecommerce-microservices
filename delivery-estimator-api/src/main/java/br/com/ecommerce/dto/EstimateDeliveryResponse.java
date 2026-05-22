package br.com.ecommerce.dto;

public record EstimateDeliveryResponse(
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