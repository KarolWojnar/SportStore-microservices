package com.shop.customer.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CustomerCreateRequest {
    private String correlationId;
    private CustomerFromOrderDto customerFromOrderDto;
}
