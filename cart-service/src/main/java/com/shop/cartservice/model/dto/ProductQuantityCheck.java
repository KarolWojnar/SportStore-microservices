package com.shop.cartservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductQuantityCheck implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String userId;
    private String productId;
    private int quantity;
}
