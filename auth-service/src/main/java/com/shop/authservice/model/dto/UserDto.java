package com.shop.authservice.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.shop.authservice.model.Roles;
import com.shop.authservice.model.entity.User;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    @Email(message = "Email is not valid.")
    @NotEmpty(message = "Email is required.")
    private String email;
    @Size(min = 8, message = "Password must be have at least 8 characters.")
    private String password;
    @Size(min = 8, message = "Confirm password must be have at least 8 characters.")
    private String confirmPassword;
    private Roles role;

    @AssertTrue(message = "Passwords must match.")
    @JsonIgnore
    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }

    public static User toUserEntity(UserDto userDto) {
        return User.builder()
                .email(userDto.getEmail())
                .password(userDto.password)
                .build();
    }


    public static UserDto toUserDto(User user) {
        return UserDto.builder()
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
