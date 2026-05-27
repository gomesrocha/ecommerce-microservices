package br.com.ecommerce.metrics;

import br.com.ecommerce.domain.PaymentStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;

@ApplicationScoped
public class PaymentMetricsService {

    @Inject
    MeterRegistry meterRegistry;

    public void recordPayment(PaymentStatus status, String provider, BigDecimal amount) {
        Counter.builder("payment_transactions")
                .description("Quantidade de transações de pagamento")
                .tag("status", status == null ? "unknown" : status.name())
                .tag("provider", provider == null ? "unknown" : provider)
                .register(meterRegistry)
                .increment();

        if (amount != null) {
            DistributionSummary.builder("payment_amount")
                    .description("Distribuição dos valores de pagamento")
                    .tag("status", status == null ? "unknown" : status.name())
                    .tag("provider", provider == null ? "unknown" : provider)
                    .register(meterRegistry)
                    .record(amount.doubleValue());
        }
    }
}