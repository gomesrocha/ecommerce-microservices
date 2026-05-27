package br.com.ecommerce.metrics;

import br.com.ecommerce.domain.NotificationChannel;
import br.com.ecommerce.domain.NotificationDeliveryStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class NotificationMetricsService {

    @Inject
    MeterRegistry meterRegistry;

    public void recordDelivery(NotificationChannel channel, NotificationDeliveryStatus status, String eventType) {
        Counter.builder("notification_deliveries")
                .description("Quantidade de entregas de notificação por canal")
                .tag("channel", channel == null ? "unknown" : channel.name())
                .tag("status", status == null ? "unknown" : status.name())
                .tag("event_type", eventType == null ? "unknown" : eventType)
                .register(meterRegistry)
                .increment();
    }
}