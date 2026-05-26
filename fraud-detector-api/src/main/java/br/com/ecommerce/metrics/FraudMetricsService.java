package br.com.ecommerce.metrics;

import br.com.ecommerce.domain.FraudStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;

@ApplicationScoped
public class FraudMetricsService {

    @Inject
    MeterRegistry meterRegistry;

    public void recordPrediction(
            FraudStatus status,
            BigDecimal riskScore,
            String source,
            String modelVersion
    ) {
        String decision = status == null ? "unknown" : status.name();
        String safeSource = safe(source);
        String safeModelVersion = safe(modelVersion);

        Counter.builder("fraud_predictions")
                .description("Quantidade de análises de fraude realizadas")
                .tag("decision", decision)
                .tag("source", safeSource)
                .tag("model_version", safeModelVersion)
                .register(meterRegistry)
                .increment();

        if (riskScore != null) {
            DistributionSummary.builder("fraud_risk_score")
                    .description("Distribuição dos scores de risco de fraude")
                    .tag("decision", decision)
                    .tag("source", safeSource)
                    .tag("model_version", safeModelVersion)
                    .register(meterRegistry)
                    .record(riskScore.doubleValue());
        }
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value;
    }
}