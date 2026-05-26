package br.com.ecommerce.client;

public record DeliveryEstimateClientRequest(
        String originState,
        String destinationState,
        Integer totalItems
) {
}