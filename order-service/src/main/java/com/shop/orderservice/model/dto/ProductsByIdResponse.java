package com.shop.orderservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductsByIdResponse {
    private String correlationId;
    private String errorMessage;
    private List<ProductOrderDto> productDto;
}
