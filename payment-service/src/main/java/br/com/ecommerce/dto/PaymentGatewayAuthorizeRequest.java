package br.com.ecommerce.dto;

import java.math.BigDecimal;

public record PaymentGatewayAuthorizeRequest(
        Long orderId,
        Long userId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String paymentToken,
        Integer installments
) {
}