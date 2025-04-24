package com.shop.notificationservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductInfoEmail {
    private String productId;
    private String name;
    private int amount;
    private BigDecimal price;
    private boolean isRated;
}
