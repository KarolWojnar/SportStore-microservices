package com.shop.orderservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderSessionRequest {
    private String correlationId;
    private String sessionId;
    private String orderId;
}
