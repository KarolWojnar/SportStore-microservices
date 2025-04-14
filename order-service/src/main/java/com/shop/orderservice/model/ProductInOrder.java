package com.shop.orderservice.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Embeddable
@NoArgsConstructor
public class ProductInOrder {
    private String productId;
    private int amount;
    private BigDecimal price;
    private boolean isRated;

    public ProductInOrder(String productId, int amount, BigDecimal price) {
        this.productId = productId;
        this.amount = amount;
        this.price = price;
        this.isRated = false;
    }
}