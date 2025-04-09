package com.shop.productservice.service;

import com.shop.productservice.exception.ProductException;
import com.shop.productservice.model.dto.CategoryDto;
import com.shop.productservice.model.dto.ProductDto;
import com.shop.productservice.model.dto.ProductsListInfo;
import com.shop.productservice.model.dto.RateProductDto;
import com.shop.productservice.model.entity.Category;
import com.shop.productservice.model.entity.Product;
import com.shop.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public ProductsListInfo getProducts(int page, int size, String sort, String direction,
                                        String search, int minPrice, int maxPrice, List<String> categories, boolean isAdmin) {
        log.info("Page: {}", page);
        log.info("Size: {}", size);
        log.info("Sort: {}", sort);
        log.info("Direction: {}", direction);
        log.info("Search: {}", search);
        log.info("Min price: {}", minPrice);
        log.info("Max price: {}", maxPrice);
        log.info("Categories: {}", categories);
        Page<Product> products = fetchProducts(page, size, sort, direction, search, minPrice, maxPrice, categories, isAdmin);
        ProductsListInfo response = new ProductsListInfo(
                products.getContent().stream().map(product -> ProductDto.toDto(product, false)).toList(),
                products.getTotalElements()
        );
        if (page == 0) {
            response.setCategories(categoryService.getCategories().stream().map(CategoryDto::getName).toList());
            response.setMaxPrice(getMaxPrice());
        }

        log.info(String.valueOf(response.getProducts().size()));
        log.info(String.valueOf(response.getCategories().size()));
        log.info(String.valueOf(response.getTotalElements()));

        return response;
    }

    private Page<Product> fetchProducts(int page, int size, String sort, String direction, String search, int minPrice, int maxPrice, List<String> categories, boolean isAdmin) {
        Sort.Direction direct = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
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

    public double getMaxPrice() {
        return productRepository.findTopByAvailableTrueAndAmountLeftGreaterThanOrderByPriceDesc(0).getPrice();
    }

    public ProductDto changeProductAvailability(String id, boolean available) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductException("Product not found."));
        product.setAvailable(available);
        return ProductDto.minEdited(productRepository.save(product));
    }

    @Transactional(rollbackFor = ProductException.class)
    public void rateProduct(RateProductDto rateProductDto) {
        Product product = productRepository.findById(rateProductDto.getProductId()).orElseThrow(() -> new ProductException("Product not found."));
        if (rateProductDto.getRating() < 1 || rateProductDto.getRating() > 5) {
            throw new ProductException("Rating must be between 1 and 5.");
        }
        int key = 0;
        double value = 0;
        for (Map.Entry<Integer, Double> entry : product.getRatings().entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
        }

        double finalRating = ((value * key) + (double) rateProductDto.getRating()) / (key + 1);
        finalRating = Math.round(finalRating * 100.0) / 100.0;

        product.getRatings().put(key + 1, finalRating);
        product.getRatings().remove(key);

        //todo: set order product as rated
//        orderService.setOrderProductAsRated(rateProductDto.getOrderId(), rateProductDto.getProductId());
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

}
