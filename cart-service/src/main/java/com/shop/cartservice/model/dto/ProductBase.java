package com.shop.cartservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductBase implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String productId;
    private int amountLeft;
    private BigDecimal price;
    private String name;
    private String imageUrl;
}
