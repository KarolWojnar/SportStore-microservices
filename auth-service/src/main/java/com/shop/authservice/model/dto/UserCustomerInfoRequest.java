package com.shop.authservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCustomerInfoRequest {
    private String correlationId;
    private List<Long> userIds;
}
