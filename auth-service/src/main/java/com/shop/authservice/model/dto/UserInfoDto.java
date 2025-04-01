package com.shop.authservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shop.authservice.model.Roles;
import com.shop.authservice.model.entity.User;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class UserInfoDto {
    private Long id;
    private String email;
    private Roles role;
    private boolean enabled;

    public static UserInfoDto mapToDto(User user) {
        UserInfoDto userInfoDto = new UserInfoDto();
        userInfoDto.setEmail(user.getEmail());
        userInfoDto.setRole(user.getRole());
        userInfoDto.setId(user.getId());
        userInfoDto.setEnabled(user.isEnabled());
        return userInfoDto;
    }
}
