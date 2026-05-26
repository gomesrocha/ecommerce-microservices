package br.com.ecommerce.domain;

public enum OrderStatus {
    CREATED,
    WAITING_STOCK,
    WAITING_FRAUD,
    CONFIRMED,
    CANCELED,
    REJECTED
}