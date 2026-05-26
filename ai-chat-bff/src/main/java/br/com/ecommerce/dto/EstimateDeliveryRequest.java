package br.com.ecommerce.dto;

public record EstimateDeliveryRequest(
        String originState,
        String destinationState,
        Integer totalItems
) {
}