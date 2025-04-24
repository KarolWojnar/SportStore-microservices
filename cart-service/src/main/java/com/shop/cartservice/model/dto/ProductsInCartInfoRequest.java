package com.shop.cartservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductsInCartInfoRequest {
    private String correlationId;
    private boolean blockCart;
    private String userId;
}
