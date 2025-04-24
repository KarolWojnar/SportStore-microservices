package com.shop.productservice.controller;

import com.shop.productservice.model.dto.CategoryDto;
import com.shop.productservice.model.dto.ProductAvailability;
import com.shop.productservice.model.dto.ProductDto;
import com.shop.productservice.model.dto.RateProductDto;
import com.shop.productservice.service.CategoryService;
import com.shop.productservice.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<?> getProducts(@RequestHeader("X-User-Role") String role,
                                         @RequestParam(value = "page", defaultValue = "0") int page,
                                         @RequestParam(value = "size", defaultValue = "6") int size,
                                         @RequestParam(value = "sort", defaultValue = "id") String sort,
                                         @RequestParam(value = "direction", defaultValue = "asc") String direction,
                                         @RequestParam(value = "search", defaultValue = "") String search,
                                         @RequestParam(value = "minPrice", defaultValue = "0") @Min(0) int minPrice,
                                         @RequestParam(value = "maxPrice", defaultValue = "9999") @Max(9999) int maxPrice,
                                         @RequestParam(value = "categories", defaultValue = "", required = false) List<String> categories) {
        return ResponseEntity
                .ok(productService
                        .getProducts(page, size, sort, direction, search, minPrice, maxPrice, categories,role));
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

    @PatchMapping("/admin/{id}/available")
    public ResponseEntity<?> changeProductAvailability(@RequestHeader("X-User-Role") @NonNull String role,
                                                       @PathVariable String id, @RequestBody ProductAvailability available) {
        return ResponseEntity.ok(productService.changeProductAvailability(role,id, available));
    }

    @PatchMapping("/admin/{id}")
    public ResponseEntity<?> changeProductData(@RequestHeader("X-User-Role") @NonNull String role,
                                               @PathVariable String id, @RequestBody ProductDto product) {
        return ResponseEntity.ok(productService.changeProductData(role, id, product));
    }

    @PostMapping(value = "/admin", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addProduct(@RequestHeader("X-User-Role") @NonNull String role,
                                        @RequestPart("product") String productJson,
                                        @RequestPart(value = "file", required = false) MultipartFile file) {
        return new ResponseEntity<>(productService.addProduct(role, productJson, file), CREATED);
    }

    @PostMapping("/admin/categories")
    public ResponseEntity<?> addCategory(@RequestHeader("X-User-Role") @NonNull String role,
                                         @RequestBody CategoryDto category) {
        return new ResponseEntity<>(productService.addCategory(role, category), CREATED);
    }

}
