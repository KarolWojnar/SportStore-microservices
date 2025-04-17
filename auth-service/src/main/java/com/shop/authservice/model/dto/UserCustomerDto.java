package com.shop.authservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCustomerDto {
    private Long id;
    private String firstName;
    private String lastName;
    private ShippingAddress shippingAddress;
}
