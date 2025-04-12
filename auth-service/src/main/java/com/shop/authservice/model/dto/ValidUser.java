package com.shop.authservice.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidUser {
    private Long userId;
    private String email;
    private String role;
}
