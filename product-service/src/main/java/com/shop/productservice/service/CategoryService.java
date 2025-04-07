package com.shop.productservice.service;

import com.shop.productservice.model.dto.CategoryDto;
import com.shop.productservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryDto> getCategories() {
        return categoryRepository.findAll().stream().map(CategoryDto::toDto).toList();
    }
}
