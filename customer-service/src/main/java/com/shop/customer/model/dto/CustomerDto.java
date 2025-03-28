package com.shop.customer.model.dto;

import com.shop.customer.model.ShippingAddress;
import lombok.Data;

@Data
public class CustomerDto {
    private String firstName;
    private String lastName;
    private ShippingAddress shippingAddress;
}
