package com.shop.orderservice.model;

import jakarta.persistence.Embeddable;

@Embeddable
public enum OrderStatus {
    CREATED,
    PROCESSING,
    SHIPPING,
    DELIVERED,
    ANNULLED,
    REFUNDED
}
