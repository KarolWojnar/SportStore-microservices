package com.shop.productservice.model.dto;

import com.shop.productservice.model.entity.Category;
import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@Data
@NoArgsConstructor
public class CategoryDto {
    private String name;

    public static CategoryDto toDto(Category category){
        return new CategoryDto(category.getName());
    }
}

