package com.shop.productservice.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.shop.productservice.model.entity.Category;
import com.shop.productservice.model.entity.Product;
import lombok.Data;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDto {
    private String name;
    private String id;
    private String description;
    private double price;
    private int quantity;
    private double rating;
    private String image;
    private boolean available;
    private int soldItems;
    private List<String> categories;

    public static ProductDto toDto(Product product, boolean withDetails) {
        ProductDto productDto = new ProductDto();
        productDto.setName(product.getName());
        productDto.setPrice(product.getPrice());
        productDto.setImage(product.getImageUrl());
        productDto.setAvailable(product.isAvailable());
        productDto.setRating(product.getRatings().values().iterator().next());
        productDto.setQuantity(product.getAmountLeft());
        productDto.setAvailable(product.isAvailable());
        productDto.setId(product.getId());
        productDto.setCategories(product.getCategories().stream().map(Category::getName).collect(toList()));
        if (withDetails) {
            productDto.setDescription(product.getDescription());
            productDto.setSoldItems(product.getOrders());
        }
        return productDto;
    }

    public static Product toEntity(ProductDto productDto, List<Category> categories) {
        Product product = new Product();
        product.setName(productDto.getName());
        product.setPrice(productDto.getPrice());
        product.setImageUrl(productDto.getImage());
        product.setAmountLeft(productDto.getQuantity());
        product.setDescription(productDto.getDescription());
        product.setOrders(0);
        product.setRatings(Map.of(0, 0.0));
        product.setCategories(categories);
        return product;
    }

    public static ProductDto minDto(Product product) {
        ProductDto productDto = new ProductDto();
        productDto.setName(product.getName());
        productDto.setPrice(product.getPrice());
        productDto.setImage(product.getImageUrl());
        productDto.setId(product.getId());
        return productDto;
    }

    public static ProductDto minEdited(Product product) {
        ProductDto productDto = new ProductDto();
        productDto.setName(product.getName());
        productDto.setPrice(product.getPrice());
        productDto.setQuantity(product.getAmountLeft());
        productDto.setId(product.getId());
        return productDto;
    }
}
