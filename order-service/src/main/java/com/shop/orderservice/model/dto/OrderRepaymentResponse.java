package com.shop.orderservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRepaymentResponse {
    private String correlationId;
    private String errorMessage;
    private OrderInfoRepayment orderInfoRepayment;
}
