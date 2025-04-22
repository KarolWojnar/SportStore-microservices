package com.shop.productservice.service;

import com.shop.productservice.model.dto.CategoryDto;
import com.shop.productservice.model.entity.Category;
import com.shop.productservice.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void getCategories_ShouldReturnAllCategories() {
        Category category1 = new Category("Category1");
        Category category2 = new Category("Category2");
        when(categoryRepository.findAll()).thenReturn(List.of(category1, category2));

        List<CategoryDto> result = categoryService.getCategories();

        assertEquals(2, result.size());
        assertEquals("Category1", result.get(0).getName());
        assertEquals("Category2", result.get(1).getName());
    }
}