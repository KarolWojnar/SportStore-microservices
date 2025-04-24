package com.shop.productservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.productservice.exception.ProductException;
import com.shop.productservice.model.dto.*;
import com.shop.productservice.model.entity.Category;
import com.shop.productservice.model.entity.Product;
import com.shop.productservice.repository.CategoryRepository;
import com.shop.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private CategoryService categoryService;

    @Mock
    private KafkaEventService kafkaEventService;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductDto testProductDto;
    private final Category category1 = new Category("Category1");

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId("1");
        testProduct.setName("Test Product");
        testProduct.setPrice(BigDecimal.TEN);
        testProduct.setAmountLeft(10);
        testProduct.setAvailable(true);
        testProduct.setCategories(List.of(category1));
        testProduct.setRatings(new HashMap<>(Map.of(4, 4.3)));

        testProductDto = new ProductDto();
        testProductDto.setId("1");
        testProductDto.setName("Test Product");
        testProductDto.setPrice(BigDecimal.TEN);
        testProductDto.setQuantity(10);
        testProductDto.setDescription("Test Description");
        testProductDto.setCategories(List.of(category1.getName()));
    }

    @Test
    void getProducts_ShouldReturnProductsListInfo() {
        Page<Product> productPage = new PageImpl<>(List.of(testProduct));
        when(productRepository.findByNameMatchesRegexIgnoreCase(anyString(), anyBoolean(), anyInt(), anyInt(), any()))
                .thenReturn(productPage);
        when(categoryService.getCategories()).thenReturn(List.of(new CategoryDto("Category1")));
        when(productRepository.findTopByAvailableTrueAndAmountLeftGreaterThanOrderByPriceDesc(anyInt()))
                .thenReturn(testProduct);

        ProductsListInfo result = productService.getProducts(0, 6, "name", "asc", "test", 0, 100, null, "ROLE_CUSTOMER");

        assertNotNull(result);
        assertEquals(1, result.getProducts().size());
        assertEquals(1, result.getCategories().size());
        assertEquals(BigDecimal.TEN, result.getMaxPrice());
    }

    @Test
    void changeProductAvailability_ShouldChangeAvailabilityForAdmin() {
        when(productRepository.findById("1")).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductAvailability availability = new ProductAvailability(false);

        ProductDto result = productService.changeProductAvailability("ROLE_ADMIN", "1", availability);

        assertNotNull(result);
        verify(productRepository).save(testProduct);
    }

    @Test
    void changeProductAvailability_ShouldThrowForNonAdmin() {
        ProductAvailability availability = new ProductAvailability(false);

        assertThrows(ProductException.class, () ->
                productService.changeProductAvailability("ROLE_USER", "1", availability));
    }

    @Test
    void rateProduct_ShouldUpdateProductRating() {
        when(productRepository.findById("1")).thenReturn(Optional.of(testProduct));
        when(kafkaEventService.setOrderProductAsRated("order1", "1"))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        RateProductDto rateDto = new RateProductDto("1", 1, "order1");

        productService.rateProduct(rateDto);

        verify(productRepository).save(any(Product.class));
        verify(kafkaEventService).setOrderProductAsRated("order1", "1");
    }

    @Test
    void getFeaturedProducts_ShouldReturnFeaturedProducts() {
        when(productRepository.findTop9ByAvailableTrueOrderByOrdersDesc()).thenReturn(List.of(testProduct));

        Map<String, Object> result = productService.getFeaturedProducts();

        assertNotNull(result);
        assertTrue(result.containsKey("products"));
        assertEquals(1, ((List<?>) result.get("products")).size());
    }

    @Test
    void getDetails_ShouldReturnProductDetails() {
        when(productRepository.findByIdAndAvailableTrue("1")).thenReturn(Optional.ofNullable(testProduct));
        when(productRepository.findTop4ByCategoriesInAndIdNotAndAvailableTrue(any(), anyString()))
                .thenReturn(List.of(testProduct));

        Map<String, Object> result = productService.getDetails("1");

        assertNotNull(result);
        assertTrue(result.containsKey("product"));
        assertTrue(result.containsKey("relatedProducts"));
    }

    @Test
    void changeProductData_ShouldUpdateProductForAdmin() {
        when(productRepository.findById("1")).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        ProductDto result = productService.changeProductData("ROLE_ADMIN", "1", testProductDto);

        assertNotNull(result);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void addProduct_ShouldCreateNewProductForAdmin() throws Exception {
        when(categoryRepository.findByNameIn(anyList())).thenReturn(List.of(new Category("Category1")));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getBytes()).thenReturn(new byte[0]);
        when(multipartFile.getOriginalFilename()).thenReturn("test.png");

        ObjectMapper mapper = new ObjectMapper();
        String productJson = mapper.writeValueAsString(testProductDto);

        ProductDto result = productService.addProduct("ROLE_ADMIN", productJson, multipartFile);

        assertNotNull(result);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void addProduct_ShouldThrowForInvalidImageType() throws Exception {
        when(multipartFile.getContentType()).thenReturn("image/jpeg");

        ObjectMapper mapper = new ObjectMapper();
        String productJson = mapper.writeValueAsString(testProductDto);

        assertThrows(RuntimeException.class, () ->
                productService.addProduct("ROLE_ADMIN", productJson, multipartFile));
    }

    @Test
    void addCategory_ShouldCreateNewCategoryForAdmin() {
        when(categoryRepository.findByName("New Category")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(new Category("New Category"));

        CategoryDto result = productService.addCategory("ROLE_ADMIN", new CategoryDto("New Category"));

        assertNotNull(result);
        assertEquals("New Category", result.getName());
    }
}