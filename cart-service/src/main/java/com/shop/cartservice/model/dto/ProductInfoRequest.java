package com.shop.cartservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductInfoRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String correlationId;
    private List<String> productIds;
}

