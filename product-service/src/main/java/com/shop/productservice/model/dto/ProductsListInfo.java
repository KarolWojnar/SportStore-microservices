package com.shop.productservice.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductsListInfo {

    private List<ProductDto> products;
    private long totalElements;
    private List<String> categories;
    private BigDecimal maxPrice;

    public ProductsListInfo(List<ProductDto> products, long totalElements) {
        this.products = products;
        this.totalElements = totalElements;
    }
}
