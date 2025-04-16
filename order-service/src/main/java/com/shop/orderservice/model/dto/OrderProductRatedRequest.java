package com.shop.orderservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderProductRatedRequest {
    private String correlationId;
    private String orderId;
    private String productId;
}
