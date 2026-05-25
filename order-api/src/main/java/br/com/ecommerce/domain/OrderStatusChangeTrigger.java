package br.com.ecommerce.domain;

public enum OrderStatusChangeTrigger {
    ORDER_CREATED,
    STOCK_RESERVED,
    STOCK_REJECTED,
    FRAUD_APPROVED,
    FRAUD_REJECTED,
    ORDER_CANCELED
}