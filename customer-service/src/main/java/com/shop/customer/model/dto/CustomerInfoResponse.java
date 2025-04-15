package com.shop.customer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerInfoResponse {
    private String correlationId;
    private CustomerDto customer;
}
