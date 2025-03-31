package com.shop.productservice.service;

import com.shop.productservice.model.entity.Category;
import com.shop.productservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<String> getCategories() {
        return categoryRepository.findAll().stream().map(Category::getName).toList();
    }
}
