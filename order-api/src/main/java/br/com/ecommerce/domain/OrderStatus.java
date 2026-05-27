package br.com.ecommerce.domain;

public enum OrderStatus {
    CREATED,
    WAITING_STOCK,
    WAITING_FRAUD,
    WAITING_PAYMENT,
    CONFIRMED,
    CANCELED,
    REJECTED
}