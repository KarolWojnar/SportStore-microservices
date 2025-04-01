package com.shop.authservice.model.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.shop.authservice.model.entity.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDetailsDto extends UserInfoDto {
    private static final Logger log = LoggerFactory.getLogger(UserDetailsDto.class);
    private String firstName;
    private String lastName;

    public static UserDetailsDto toDto(User user) {
        UserDetailsDto userDetailsDto = new UserDetailsDto();
        userDetailsDto.setId(user.getId());
        userDetailsDto.setEmail(user.getEmail());
        userDetailsDto.setRole(user.getRole());
        userDetailsDto.setEnabled(user.isEnabled());
        return userDetailsDto;
    }
}
