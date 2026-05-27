package br.com.ecommerce.dto;

import br.com.ecommerce.domain.Payment;
import br.com.ecommerce.domain.PaymentMethod;
import br.com.ecommerce.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String eventId,
        Long orderId,
        Long userId,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        Integer installments,
        String provider,
        String providerTransactionId,
        String authorizationCode,
        PaymentStatus status,
        String reason,
        Integer attempts,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt
) {
    public static PaymentResponse fromEntity(Payment payment) {
        return new PaymentResponse(
                payment.id,
                payment.eventId,
                payment.orderId,
                payment.userId,
                payment.amount,
                payment.currency,
                payment.paymentMethod,
                payment.installments,
                payment.provider,
                payment.providerTransactionId,
                payment.authorizationCode,
                payment.status,
                payment.reason,
                payment.attempts,
                payment.createdAt,
                payment.updatedAt,
                payment.approvedAt,
                payment.rejectedAt
        );
    }
}