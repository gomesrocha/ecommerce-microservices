package br.com.ecommerce.notification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record NotificationRequestEvent(
        String eventId,
        String eventType,
        String aggregateType,
        Long aggregateId,
        Long userId,
        String title,
        String message,
        String email,
        String severity,
        List<String> channels,
        Map<String, Object> metadata
) {
    public static NotificationRequestEvent orderConfirmed(
            Long orderId,
            Long userId,
            BigDecimal totalAmount,
            String customerState
    ) {
        return new NotificationRequestEvent(
                UUID.randomUUID().toString(),
                "ORDER_CONFIRMED",
                "ORDER",
                orderId,
                userId,
                "Pedido confirmado",
                "Seu pedido " + orderId + " foi confirmado com sucesso. Valor total: R$ " + totalAmount + ".",
                defaultEmail(userId),
                "INFO",
                List.of("SCREEN", "EMAIL"),
                Map.of(
                        "orderId", orderId,
                        "totalAmount", totalAmount,
                        "customerState", customerState
                )
        );
    }

    public static NotificationRequestEvent orderCanceled(
            Long orderId,
            Long userId,
            String reason
    ) {
        return new NotificationRequestEvent(
                UUID.randomUUID().toString(),
                "ORDER_CANCELED",
                "ORDER",
                orderId,
                userId,
                "Pedido cancelado",
                "Seu pedido " + orderId + " foi cancelado. Motivo: " + safe(reason) + ".",
                defaultEmail(userId),
                "WARNING",
                List.of("SCREEN", "EMAIL"),
                Map.of(
                        "orderId", orderId,
                        "reason", safe(reason)
                )
        );
    }

    public static NotificationRequestEvent orderRejected(
            Long orderId,
            Long userId,
            String reason
    ) {
        return new NotificationRequestEvent(
                UUID.randomUUID().toString(),
                "ORDER_REJECTED",
                "ORDER",
                orderId,
                userId,
                "Pedido rejeitado",
                "Seu pedido " + orderId + " foi rejeitado. Motivo: " + safe(reason) + ".",
                defaultEmail(userId),
                "ERROR",
                List.of("SCREEN", "EMAIL"),
                Map.of(
                        "orderId", orderId,
                        "reason", safe(reason)
                )
        );
    }

    private static String defaultEmail(Long userId) {
        return "cliente@ecommerce.local";
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "Não informado" : value;
    }
}