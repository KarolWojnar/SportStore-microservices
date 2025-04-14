package com.shop.customer.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shop.customer.model.ShippingAddress;
import com.shop.customer.model.entity.Customer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDetailsDto extends UserInfoDto {
    private String firstName;
    private String lastName;
    private ShippingAddress shippingAddress;

    public static UserDetailsDto toDto(UserInfoDto user, Customer customer) {
        UserDetailsDto userDetailsDto = new UserDetailsDto();
        userDetailsDto.setId(user.getId());
        userDetailsDto.setEmail(user.getEmail());
        userDetailsDto.setRole(user.getRole());
        userDetailsDto.setEnabled(user.isEnabled());
        if (customer == null) {
            return userDetailsDto;
        }
        userDetailsDto.setFirstName(customer.getFirstName());
        userDetailsDto.setLastName(customer.getLastName());
        userDetailsDto.setShippingAddress(customer.getShippingAddress());
        return userDetailsDto;
    }
}
