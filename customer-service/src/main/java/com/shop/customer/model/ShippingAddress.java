package com.shop.customer.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ShippingAddress {
    private String address;
    private String city;
    private String country;
    private String zipCode;
}
