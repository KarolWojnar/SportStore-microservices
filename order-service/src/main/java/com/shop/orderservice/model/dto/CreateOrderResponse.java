package com.shop.orderservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateOrderResponse {
    private String correlationId;
    private String orderId;
}
