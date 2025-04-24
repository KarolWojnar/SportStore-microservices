package com.shop.productservice.service;

import com.shop.productservice.model.dto.ProductDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilTest {

    @Test
    void validProductData_ShouldNotThrowForValidProduct() {
        ProductDto validProduct = new ProductDto();
        validProduct.setName("Test Product");
        validProduct.setPrice(BigDecimal.TEN);
        validProduct.setQuantity(10);

        assertDoesNotThrow(() -> ValidationUtil.validProductData(validProduct));
    }

    @Test
    void validProductData_ShouldThrowWhenNameIsEmpty() {
        ProductDto invalidProduct = new ProductDto();
        invalidProduct.setName("");
        invalidProduct.setPrice(BigDecimal.TEN);
        invalidProduct.setQuantity(10);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtil.validProductData(invalidProduct));
        assertEquals("Product name is required.", exception.getMessage());
    }

    @Test
    void validProductData_ShouldThrowWhenPriceIsZero() {
        ProductDto invalidProduct = new ProductDto();
        invalidProduct.setName("Test Product");
        invalidProduct.setPrice(BigDecimal.ZERO);
        invalidProduct.setQuantity(10);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtil.validProductData(invalidProduct));
        assertEquals("Product price must be greater than 0.", exception.getMessage());
    }

    @Test
    void validProductData_ShouldThrowWhenQuantityIsNegative() {
        ProductDto invalidProduct = new ProductDto();
        invalidProduct.setName("Test Product");
        invalidProduct.setPrice(BigDecimal.TEN);
        invalidProduct.setQuantity(-1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtil.validProductData(invalidProduct));
        assertEquals("Product quantity must be non-negative.", exception.getMessage());
    }

    @Test
    void validRestProduct_ShouldNotThrowForValidProduct() {
        ProductDto validProduct = new ProductDto();
        validProduct.setDescription("Test Description");
        validProduct.setCategories(List.of("Category1", "Category2"));

        assertDoesNotThrow(() -> ValidationUtil.validRestProduct(validProduct));
    }

    @Test
    void validRestProduct_ShouldThrowWhenDescriptionIsEmpty() {
        ProductDto invalidProduct = new ProductDto();
        invalidProduct.setDescription("");
        invalidProduct.setCategories(List.of("Category1"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtil.validRestProduct(invalidProduct));
        assertEquals("Product description is required.", exception.getMessage());
    }

    @Test
    void validRestProduct_ShouldThrowWhenCategoriesAreEmpty() {
        ProductDto invalidProduct = new ProductDto();
        invalidProduct.setDescription("Test Description");
        invalidProduct.setCategories(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ValidationUtil.validRestProduct(invalidProduct));
        assertEquals("Product categories are required.", exception.getMessage());
    }
}