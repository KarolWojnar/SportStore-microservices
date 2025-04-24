package com.shop.productservice.model.dto;

import com.shop.productservice.model.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductBase {
    private String productId;
    private int amountLeft;
    private BigDecimal price;
    private String name;
    private String imageUrl;

    public static ProductBase mapToBase(Product product) {
        return new ProductBase(
                product.getId(),
                product.getAmountLeft(),
                product.getPrice(),
                product.getName(),
                product.getImageUrl()
        );
    }
}
