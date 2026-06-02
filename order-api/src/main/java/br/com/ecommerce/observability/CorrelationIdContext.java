package br.com.ecommerce.observability;

import java.util.UUID;

public final class CorrelationIdContext {

    public static final String HEADER_NAME = "X-Correlation-Id";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private CorrelationIdContext() {
    }

    public static String get() {
        return CURRENT.get();
    }

    public static String getOrCreate() {
        String correlationId = CURRENT.get();

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            CURRENT.set(correlationId);
        }

        return correlationId;
    }

    public static void set(String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            CURRENT.set(UUID.randomUUID().toString());
            return;
        }

        CURRENT.set(correlationId.trim());
    }

    public static void clear() {
        CURRENT.remove();
    }
}