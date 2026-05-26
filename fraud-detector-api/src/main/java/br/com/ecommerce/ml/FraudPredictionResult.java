package br.com.ecommerce.ml;

public record FraudPredictionResult(
        String label,
        boolean fraudRisk,
        double riskScore,
        String reason,
        String modelVersion
) {
}