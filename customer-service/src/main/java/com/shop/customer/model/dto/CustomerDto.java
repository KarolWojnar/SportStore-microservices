package com.shop.customer.model.dto;

import com.shop.customer.model.ShippingAddress;
import com.shop.customer.model.entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {
    private String firstName;
    private String lastName;
    private ShippingAddress shippingAddress;

    public static CustomerDto toDto(Customer customer) {
        if (customer == null) {
            return null;
        }
        return new CustomerDto(customer.getFirstName(), customer.getLastName(), customer.getShippingAddress());
    }
}
