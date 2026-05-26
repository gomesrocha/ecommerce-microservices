package br.com.ecommerce.ml;

public record DeliveryPredictionResult(
        Integer minDays,
        Integer estimatedDays,
        Integer maxDays,
        String source,
        String modelVersion
) {
}