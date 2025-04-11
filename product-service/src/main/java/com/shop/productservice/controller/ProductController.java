package com.shop.productservice.controller;

import com.shop.productservice.model.dto.RateProductDto;
import com.shop.productservice.service.CategoryService;
import com.shop.productservice.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<?> getProducts(@RequestParam(value = "page", defaultValue = "0") int page,
                                         @RequestParam(value = "size", defaultValue = "6") int size,
                                         @RequestParam(value = "sort", defaultValue = "id") String sort,
                                         @RequestParam(value = "direction", defaultValue = "asc") String direction,
                                         @RequestParam(value = "search", defaultValue = "") String search,
                                         @RequestParam(value = "minPrice", defaultValue = "0") @Min(0) int minPrice,
                                         @RequestParam(value = "maxPrice", defaultValue = "9999") @Max(9999) int maxPrice,
                                         @RequestParam(value = "categories", defaultValue = "", required = false) List<String> categories) {
        return ResponseEntity
                .ok(productService
                        .getProducts(page, size, sort, direction, search, minPrice, maxPrice, categories, false));
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(categoryService.getCategories());
    }

    @GetMapping("/featured")
    public ResponseEntity<?> getFeaturedProducts() {
        return ResponseEntity.ok(productService.getFeaturedProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductDetails(@PathVariable String id) {
        return ResponseEntity.ok(productService.getDetails(id));
    }

    @PatchMapping("/rate")
    public ResponseEntity<?> rateProduct(@Valid @RequestBody RateProductDto rateProductDto) {
        productService.rateProduct(rateProductDto);
        return ResponseEntity.noContent().build();
    }

}
