package br.com.ecommerce.service;

public record StockReservationResult(
        boolean reserved,
        String reason
) {

    public static StockReservationResult reserved(String reason) {
        return new StockReservationResult(true, reason);
    }

    public static StockReservationResult rejected(String reason) {
        return new StockReservationResult(false, reason);
    }
}