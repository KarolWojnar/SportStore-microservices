package com.shop.productservice.controller;

import com.shop.productservice.model.ErrorResponse;
import com.shop.productservice.model.dto.RateProductDto;
import com.shop.productservice.service.CategoryService;
import com.shop.productservice.service.ProductService;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Min;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProducts(@RequestParam(value = "page", defaultValue = "0") int page,
                                         @RequestParam(value = "size", defaultValue = "6") int size,
                                         @RequestParam(value = "sort", defaultValue = "id") String sort,
                                         @RequestParam(value = "direction", defaultValue = "asc") String direction,
                                         @RequestParam(value = "search", defaultValue = "") String search,
                                         @RequestParam(value = "minPrice", defaultValue = "0") @Min(0) int minPrice,
                                         @RequestParam(value = "maxPrice", defaultValue = "9999") @Max(9999) int maxPrice,
                                         @RequestParam(value = "categories", defaultValue = "", required = false) List<String> categories) {
        try {
            return ResponseEntity.ok(productService.getProducts(page, size, sort, direction, search, minPrice, maxPrice, categories, false));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @Cacheable("maxPrice")
    @GetMapping("/max-price")
    public ResponseEntity<?> getMaxPrice() {
        try {
            return ResponseEntity.ok(Map.of("maxPrice", productService.getMaxPrice()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(Map.of("categories", categoryService.getCategories()));
    }

    @GetMapping("/featured")
    public ResponseEntity<?> getFeaturedProducts() {
        try {
            return ResponseEntity.ok(productService.getFeaturedProducts());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductDetails(@PathVariable String id) {
        try {
            return ResponseEntity.ok(productService.getDetails(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/rate")
    public ResponseEntity<?> rateProduct(@RequestBody RateProductDto rateProductDto) {
        try {
            productService.rateProduct(rateProductDto);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

}
