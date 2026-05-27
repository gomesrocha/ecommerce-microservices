package br.com.ecommerce.domain;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
    CREATED,
    WAITING_STOCK,
    WAITING_FRAUD,
    WAITING_PAYMENT,
    CONFIRMED,
    REJECTED,
    CANCELED
}