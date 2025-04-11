package com.shop.cartservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductBase {
    private String productId;
    private int amountLeft;
    private BigDecimal price;
    private String name;
    private String imageUrl;
}
