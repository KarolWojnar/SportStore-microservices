package com.shop.customer.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class UserInfoDto {
    private String id;
    private String email;
    private String role;
    private boolean enabled;
}
