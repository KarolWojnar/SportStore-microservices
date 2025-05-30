package com.shop.orderservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TotalPriceOfProductsResponse {
    private String correlationId;
    private BigDecimal totalPrice;
    private String errorMessage;
}
