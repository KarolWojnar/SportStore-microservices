package com.shop.orderservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductPriceByIdResponse {
    private String correlationId;
    private String errorMessage;
    private Map<String, BigDecimal> productPriceDto;
}
