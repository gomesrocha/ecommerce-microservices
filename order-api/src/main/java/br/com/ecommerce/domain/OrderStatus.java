package br.com.ecommerce.domain;

public enum OrderStatus {
    CREATED,
    WAITING_FRAUD,
    CONFIRMED,
    CANCELED,
    REJECTED
}