package com.shop.orderservice.model.dto;

import com.shop.orderservice.model.ShippingAddress;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerDto {
    private String firstName;
    private String lastName;
    private ShippingAddress shippingAddress;
}
