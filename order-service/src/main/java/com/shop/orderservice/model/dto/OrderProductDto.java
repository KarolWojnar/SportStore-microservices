package com.shop.orderservice.model.dto;

import com.shop.orderservice.exception.OrderException;
import com.shop.orderservice.model.ProductInOrder;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrderProductDto {
    private String productId;
    private int quantity;
    private BigDecimal price;
    private String name;
    private String image;
    private boolean isRated;


    public static List<OrderProductDto> mapToDto(List<ProductInOrder> products, List<ProductOrderDto> productList) {
        return products.stream().map(product -> {
            ProductOrderDto product1 = productList.stream()
                    .filter(p -> p.getId().equals(product.getProductId()))
                    .findFirst().orElseThrow(() -> new OrderException("Product not found"));
            return OrderProductDto.builder()
                    .productId(product.getProductId())
                    .quantity(product.getAmount())
                    .price(product.getPrice())
                    .name(product1.getName())
                    .image(product1.getImageUrl())
                    .isRated(product.isRated())
                    .build();
        }).toList();
    }
}
