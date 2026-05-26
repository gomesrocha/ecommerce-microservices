package br.com.ecommerce.metrics;

import br.com.ecommerce.dto.EstimateDeliveryResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DeliveryMetricsService {

    @Inject
    MeterRegistry meterRegistry;

    public void recordPrediction(EstimateDeliveryResponse response) {
        String source = safe(response.source());
        String modelVersion = safe(response.modelVersion());

        Counter.builder("delivery_predictions")
                .description("Quantidade de predições de entrega realizadas")
                .tag("source", source)
                .tag("model_version", modelVersion)
                .register(meterRegistry)
                .increment();

        if (response.estimatedDays() != null) {
            DistributionSummary.builder("delivery_estimated_days")
                    .description("Distribuição dos prazos estimados de entrega")
                    .tag("source", source)
                    .tag("model_version", modelVersion)
                    .register(meterRegistry)
                    .record(response.estimatedDays());
        }
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value;
    }
}