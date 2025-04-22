package com.shop.productservice.controller;

import com.shop.productservice.model.dto.*;
import com.shop.productservice.service.CategoryService;
import com.shop.productservice.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private ProductController productController;

    @Test
    void getProducts_ShouldReturnProducts() {
        ProductsListInfo expectedResponse = new ProductsListInfo(List.of(), 0);
        when(productService.getProducts(anyInt(), anyInt(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyList(), anyString())).thenReturn(expectedResponse);

        ResponseEntity<?> response = productController.getProducts("ROLE_USER", 0, 6, "name", "asc",
                "test", 0, 100, List.of());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedResponse, response.getBody());
    }

    @Test
    void getCategories_ShouldReturnCategories() {
        List<CategoryDto> expectedCategories = List.of(new CategoryDto("Category1"));
        when(categoryService.getCategories()).thenReturn(expectedCategories);

        ResponseEntity<?> response = productController.getCategories();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedCategories, response.getBody());
    }

    @Test
    void getFeaturedProducts_ShouldReturnFeaturedProducts() {
        Map<String, Object> expectedProducts = Map.of("products", List.of());
        when(productService.getFeaturedProducts()).thenReturn(expectedProducts);

        ResponseEntity<?> response = productController.getFeaturedProducts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedProducts, response.getBody());
    }

    @Test
    void getProductDetails_ShouldReturnProductDetails() {
        Map<String, Object> expectedDetails = Map.of("product", new ProductDto());
        when(productService.getDetails("1")).thenReturn(expectedDetails);

        ResponseEntity<?> response = productController.getProductDetails("1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedDetails, response.getBody());
    }

    @Test
    void rateProduct_ShouldReturnNoContent() {
        RateProductDto rateDto = new RateProductDto("order1", 3, "5");

        ResponseEntity<?> response = productController.rateProduct(rateDto);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void changeProductAvailability_ShouldReturnUpdatedProduct() {
        ProductAvailability productAvailability = new ProductAvailability(true);
        ProductDto expectedProduct = new ProductDto();
        when(productService.changeProductAvailability("ROLE_ADMIN", "1", productAvailability))
                .thenReturn(expectedProduct);

        ResponseEntity<?> response = productController.changeProductAvailability("ROLE_ADMIN", "1", productAvailability);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedProduct, response.getBody());
    }

    @Test
    void addProduct_ShouldReturnCreatedProduct() {
        ProductDto expectedProduct = new ProductDto();
        when(productService.addProduct(eq("ROLE_ADMIN"), anyString(), any())).thenReturn(expectedProduct);

        ResponseEntity<?> response = productController.addProduct("ROLE_ADMIN", "{}", multipartFile);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedProduct, response.getBody());
    }

    @Test
    void addCategory_ShouldReturnCreatedCategory() {
        CategoryDto expectedCategory = new CategoryDto("New Category");
        when(productService.addCategory("ROLE_ADMIN", new CategoryDto("New Category")))
                .thenReturn(expectedCategory);

        ResponseEntity<?> response = productController.addCategory("ROLE_ADMIN", new CategoryDto("New Category"));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(expectedCategory, response.getBody());
    }
}