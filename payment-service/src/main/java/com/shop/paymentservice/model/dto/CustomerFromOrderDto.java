package com.shop.paymentservice.model.dto;

import com.shop.paymentservice.model.ShippingAddress;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CustomerFromOrderDto {
    private Long userId;
    private String firstName;
    private String lastName;
    private ShippingAddress address;
}
