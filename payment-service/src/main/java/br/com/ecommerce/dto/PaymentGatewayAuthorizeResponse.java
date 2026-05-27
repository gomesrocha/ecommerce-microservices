package br.com.ecommerce.dto;

public record PaymentGatewayAuthorizeResponse(
        String status,
        String provider,
        String transactionId,
        String authorizationCode,
        String reason
) {
    public boolean approved() {
        return "APPROVED".equalsIgnoreCase(status);
    }
}