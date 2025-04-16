package com.shop.productservice.model.dto;

import com.shop.productservice.model.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductOrderDto {
    private String id;
    private String name;
    private String imageUrl;

    public static ProductOrderDto mapToDto(Product product) {
        return new ProductOrderDto(
                product.getId(),
                product.getName(),
                product.getImageUrl()
        );
    }
}
