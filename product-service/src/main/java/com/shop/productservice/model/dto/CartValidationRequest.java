package com.shop.productservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CartValidationRequest {
    private String correlationId;
    private Map<String, Integer> products;
}
