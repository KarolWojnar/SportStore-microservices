package com.shop.productservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.productservice.exception.ProductException;
import com.shop.productservice.model.dto.*;
import com.shop.productservice.model.entity.Category;
import com.shop.productservice.model.entity.Product;
import com.shop.productservice.repository.CategoryRepository;
import com.shop.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final KafkaEventService kafkaEventService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CategoryRepository categoryRepository;

    public ProductsListInfo getProducts(int page, int size, String sort, String direction,
                                        String search, int minPrice, int maxPrice, List<String> categories, String role) {
        Page<Product> products = fetchProducts(page, size, sort, direction, search, minPrice, maxPrice, categories, role);
        ProductsListInfo response = new ProductsListInfo(
                products.getContent().stream().map(product -> ProductDto.toDto(product, false)).toList(),
                products.getTotalElements()
        );
        log.info(String.valueOf(products.getTotalElements()));
        if (page == 0) {
            response.setCategories(categoryService.getCategories().stream().map(CategoryDto::getName).toList());
            response.setMaxPrice(getMaxPrice());
        }

        return response;
    }

    private Page<Product> fetchProducts(int page, int size, String sort, String direction, String search, int minPrice, int maxPrice, List<String> categories, String role) {
        Sort.Direction direct = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        boolean isAdmin = role.equalsIgnoreCase("ROLE_ADMIN");
        Sort sortObj = Sort.by(direct, sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<Product> products;
        if (categories == null || categories.isEmpty()) {
            products = productRepository.findByNameMatchesRegexIgnoreCase(".*" + search + ".*", !isAdmin, minPrice, maxPrice, pageable);
        } else {
            products = productRepository.findByNameMatchesRegexIgnoreCaseAndCategoriesIn(".*" + search + ".*", categories, !isAdmin, minPrice, maxPrice, pageable);
        }
        return products;
    }

    public BigDecimal getMaxPrice() {
        return productRepository.findTopByAvailableTrueAndAmountLeftGreaterThanOrderByPriceDesc(0).getPrice();
    }

    public ProductDto changeProductAvailability(String role, String id, ProductAvailability available) {
        if (!role.equalsIgnoreCase("ROLE_ADMIN")) throw new ProductException("User is not an admin.");
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductException("Product not found."));
        product.setAvailable(available.isAvailable());
        return ProductDto.minEdited(productRepository.save(product));
    }

    @Transactional(rollbackFor = ProductException.class)
    public void rateProduct(RateProductDto rateProductDto) {
        Product product = productRepository.findById(rateProductDto.getProductId())
                .orElseThrow(() -> new ProductException("Product not found."));

        Map<Integer, Double> ratings = product.getRatings();
        int totalRatings = ratings.isEmpty() ? 0 : Collections.max(ratings.keySet());
        double currentAvg = ratings.getOrDefault(totalRatings, 0.0);

        double finalRating = ((totalRatings * currentAvg) + (double) rateProductDto.getRating()) / (totalRatings + 1);
        finalRating = Math.round(finalRating * 100.0) / 100.0;

        double finalRating1 = finalRating;
        product.getRatings().putIfAbsent(totalRatings + 1, finalRating1);
        product.getRatings().remove(totalRatings);

        try {
            kafkaEventService
                    .setOrderProductAsRated(rateProductDto.getOrderId(), rateProductDto.getProductId())
                    .get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new ProductException("Error while rating product.");
        }
        productRepository.save(product);
    }

    public Map<String, Object> getFeaturedProducts() {
        List<Product> products = productRepository.findTop9ByAvailableTrueOrderByOrdersDesc();
        log.info(String.valueOf(products.size()));
        return Map.of("products", products.stream().map(product -> ProductDto.toDto(product, false)).toList());
    }

    public Map<String, Object> getDetails(String id) {
        Product product = productRepository.findByIdAndAvailableTrue(id).orElseThrow(() -> new ProductException("Product not found."));
        Collection<List<Category>> categories = Collections.singleton(product.getCategories());
        List<Product> relatedProducts = productRepository.findTop4ByCategoriesInAndIdNotAndAvailableTrue(categories, id);
        return Map.of(
                "product", ProductDto.toDto(product, true),
                "relatedProducts", relatedProducts.stream().map(ProductDto::minDto).toList());
    }

    public ProductDto changeProductData(String role, String id, ProductDto productDto) {
        if (!role.equalsIgnoreCase("ROLE_ADMIN")) throw new ProductException("User is not an admin.");
        if (!productDto.getId().equals(id)) {
            throw new ProductException("Product id must be the same.");
        }
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductException("Product not found."));
        if (productDto.getPrice() != null && productDto.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            product.setPrice(productDto.getPrice().setScale(2, RoundingMode.HALF_UP));
        }

        if (productDto.getName() != null && !productDto.getName().isEmpty()) {
            product.setName(productDto.getName());
        }

        if (productDto.getQuantity() >= 0) {
            product.setAmountLeft(productDto.getQuantity());
        }
        return ProductDto.minEdited(productRepository.save(product));
    }

    @Transactional
    public ProductDto addProduct(String role, String productJson, MultipartFile image) {
        if (!role.equalsIgnoreCase("ROLE_ADMIN")) throw new ProductException("User is not an admin.");
        ProductDto product;
        try {
            product = new ObjectMapper().readValue(productJson, ProductDto.class);
        } catch (JsonProcessingException e) {
            throw new ProductException("Error parsing product data: " + e.getMessage());
        }

        ValidationUtil.validProductData(product);
        ValidationUtil.validRestProduct(product);

        product.setImage(saveImage(image));

        List<Category> categories = categoryRepository.findByNameIn(product.getCategories());

        return ProductDto.minDto(productRepository.save(ProductDto.toEntity(product, categories)));
    }

    protected String saveImage(MultipartFile image) {
        if (image == null || image.isEmpty()) return null;

        if (!"image/png".equals(image.getContentType())) {
            throw new RuntimeException("Allowed files: PNG!");
        }

        try {
            String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();

            FileUploadMessage fileMessage = new FileUploadMessage(
                    fileName,
                    image.getContentType(),
                    image.getBytes()
            );

            kafkaTemplate.send("file-uploads", fileName, fileMessage);


            return "uploads/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + e.getMessage());
        }
    }

    public CategoryDto addCategory(String role, CategoryDto category) {
        if (!role.equalsIgnoreCase("ROLE_ADMIN")) throw new ProductException("User is not an admin.");
        Optional<Category> categoryOptional = categoryRepository.findByName(category.getName());
        if (categoryOptional.isPresent()) {
            throw new ProductException("Category already exists.");
        }
        return new CategoryDto(categoryRepository.save(new Category(category.getName())).getName());
    }
}
