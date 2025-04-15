package com.shop.paymentservice.model;

public enum OrderStatus {
    CREATED,
    PROCESSING,
    SHIPPING,
    DELIVERED,
    ANNULLED,
    REFUNDED
}