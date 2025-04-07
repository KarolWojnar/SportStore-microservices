package com.shop.productservice.model.dto;

import com.shop.productservice.model.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class CategoryDto {
    private String name;

    public static CategoryDto toDto(Category category){
        return new CategoryDto(category.getName());
    }
}

