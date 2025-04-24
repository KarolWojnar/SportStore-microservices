package com.shop.customer.model.dto;

import com.shop.customer.model.ShippingAddress;
import com.shop.customer.model.entity.Customer;
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

    public static UserCustomerDto toDto(Customer customer) {
        return new UserCustomerDto(
                customer.getUserId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getShippingAddress());
    }
}
