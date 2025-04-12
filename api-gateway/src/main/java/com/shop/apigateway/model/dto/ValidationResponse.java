package com.shop.apigateway.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidationResponse {
    private Long userId;
    private String email;
    private String role;
}