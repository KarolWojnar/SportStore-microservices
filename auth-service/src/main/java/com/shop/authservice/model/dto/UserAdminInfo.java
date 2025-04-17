package com.shop.authservice.model.dto;

import com.shop.authservice.model.Roles;
import com.shop.authservice.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserAdminInfo {
    private Long id;
    private String email;
    private Roles role;
    private boolean enabled;
    private String firstName;
    private String lastName;
    private ShippingAddress shippingAddress;

    public static UserAdminInfo toDto(User user, UserCustomerDto customerDto) {
        UserAdminInfo userDetailsDto = new UserAdminInfo();
        userDetailsDto.setId(user.getId());
        userDetailsDto.setEmail(user.getEmail());
        userDetailsDto.setRole(user.getRole());
        userDetailsDto.setEnabled(user.isEnabled());
        if (customerDto == null) {
            return userDetailsDto;
        }
        userDetailsDto.setFirstName(customerDto.getFirstName());
        userDetailsDto.setLastName(customerDto.getLastName());
        userDetailsDto.setShippingAddress(customerDto.getShippingAddress());
        return userDetailsDto;
    }
}
