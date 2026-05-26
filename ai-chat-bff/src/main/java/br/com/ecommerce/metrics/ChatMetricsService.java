package br.com.ecommerce.metrics;

import br.com.ecommerce.service.ChatIntent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ChatMetricsService {

    @Inject
    MeterRegistry meterRegistry;

    public void recordGuardrail(ChatIntent intent) {
        record("guardrail", intent, "guardrail");
    }

    public void recordDeterministicRouter(ChatIntent intent) {
        record("deterministic-router", intent, "deterministic-router");
    }

    public void recordLlm(ChatIntent intent, String model) {
        record("llm", intent, safe(model));
    }

    public void recordError(ChatIntent intent) {
        record("error-handler", intent, "error-handler");
    }

    private void record(String route, ChatIntent intent, String model) {
        Counter.builder("ai_chat_requests")
                .description("Quantidade de requisições processadas pelo AI Chat BFF")
                .tag("route", safe(route))
                .tag("intent", intent == null ? "unknown" : intent.name())
                .tag("model", safe(model))
                .register(meterRegistry)
                .increment();
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value;
    }
}