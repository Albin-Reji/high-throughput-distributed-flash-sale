package com.project_aegis.order_service.entity;

public enum OrderStatus {
    PENDING,
    AWAITING_PAYMENT,
    PAID,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED,
    FAILED
}
