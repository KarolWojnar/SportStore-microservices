package com.shop.productservice;

import com.shop.productservice.exception.ProductException;
import com.shop.productservice.model.dto.*;
import com.shop.productservice.model.entity.Category;
import com.shop.productservice.model.entity.Product;
import com.shop.productservice.repository.CategoryRepository;
import com.shop.productservice.repository.ProductRepository;
import com.shop.productservice.service.KafkaEventService;
import com.shop.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Testcontainers
public class ProductServiceIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:8.0.6");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockitoBean
    private KafkaEventService kafkaEventService;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void whenAddProduct_thenProductIsSaved() {
        Category category = new Category("Electronics");
        categoryRepository.save(category);
        String productJson = "{\"name\":\"Laptop\",\"price\":999.99,\"quantity\":10,\"description\":\"Powerful gaming laptop\",\"categories\":[\"Electronics\"]}";

        ProductDto result = productService.addProduct("ROLE_ADMIN", productJson, null);

        assertNotNull(result.getId());
        assertEquals("Laptop", result.getName());
        assertEquals(1, productRepository.count());
    }

    @Test
    void whenGetProductDetails_thenReturnProductWithRelated() {
        Category category = new Category("Electronics");
        categoryRepository.save(category);

        Product mainProduct = new Product();
        mainProduct.setName("Main Product");
        mainProduct.setAvailable(true);
        mainProduct.setRatings(Map.of(4,3.5));
        mainProduct.setCategories(List.of(category));
        productRepository.save(mainProduct);

        for (int i = 0; i < 4; i++) {
            Product related = new Product();
            related.setName("Related " + i);
            related.setAvailable(true);
            related.setCategories(List.of(category));
            productRepository.save(related);
        }

        Map<String, Object> result = productService.getDetails(mainProduct.getId());

        assertNotNull(result);
        assertEquals("Main Product", ((ProductDto) result.get("product")).getName());
        assertEquals(4, ((List<?>) result.get("relatedProducts")).size());
    }

    @Test
    void whenChangeProductAvailability_thenAvailabilityIsUpdated() {
        Product product = new Product();
        product.setName("Test Product");
        product.setAvailable(false);
        product.setRatings(Map.of(4,3.5));
        product = productRepository.save(product);

        ProductAvailability availability = new ProductAvailability();
        availability.setAvailable(true);

        ProductDto result = productService.changeProductAvailability("ROLE_ADMIN", product.getId(), availability);

        assertTrue(result.isAvailable());
        Optional<Product> updated = productRepository.findById(product.getId());
        assertTrue(updated.isPresent());
        assertTrue(updated.get().isAvailable());
    }

    @Test
    void whenRateProduct_thenRatingIsUpdated() {
        Product product = new Product();
        product.setName("Test Product");
        product.setRatings(new HashMap<>());
        product = productRepository.save(product);

        RateProductDto rateDto = new RateProductDto();
        rateDto.setProductId(product.getId());
        rateDto.setRating(5);
        rateDto.setOrderId("order-123");

        when(kafkaEventService.setOrderProductAsRated(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        productService.rateProduct(rateDto);

        Optional<Product> updated = productRepository.findById(product.getId());
        assertTrue(updated.isPresent());
        assertEquals(5.0, updated.get().getRatings().get(1));
        verify(kafkaEventService).setOrderProductAsRated("order-123", product.getId());
    }

    @Test
    void whenRateProductWithKafkaTimeout_thenThrowException() {
        Product product = new Product();
        product.setName("Test Product");
        product.setRatings(new HashMap<>());
        product = productRepository.save(product);

        RateProductDto rateDto = new RateProductDto();
        rateDto.setProductId(product.getId());
        rateDto.setRating(5);
        rateDto.setOrderId("order-123");

        when(kafkaEventService.setOrderProductAsRated(any(), any()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException()));

        assertThrows(ProductException.class, () -> productService.rateProduct(rateDto));
    }

    @Test
    void whenGetProductsWithFilters_thenReturnFilteredResults() {
        Category electronics = new Category("Electronics");
        Category books = new Category("Books");
        categoryRepository.saveAll(List.of(electronics, books));

        for (int i = 0; i < 5; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setPrice(BigDecimal.valueOf(100 * (i + 1)));
            product.setAvailable(true);
            product.setAmountLeft(i);
            product.setRatings(Map.of(1, 1.0 + i));
            product.setCategories(i % 2 == 0 ? List.of(electronics) : List.of(books));
            productRepository.save(product);
        }

        ProductsListInfo result = productService.getProducts(
                0, 10, "price", "asc", "Product", 200, 500,
                List.of("Electronics"), "ROLE_CUSTOMER");

        assertEquals(2, result.getProducts().size());
        assertEquals(300.0, result.getProducts().get(0).getPrice().doubleValue());
        assertEquals(500.0, result.getProducts().get(1).getPrice().doubleValue());
        assertTrue(result.getCategories().contains("Electronics"));
        assertTrue(result.getCategories().contains("Books"));
    }

    @Test
    void whenGetFeaturedProducts_thenReturnMostOrderedProducts() {
        for (int i = 0; i < 10; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setRatings(Map.of(4,3.5));
            product.setAvailable(true);
            product.setCategories(List.of(new Category("Electronics")));
            product.setOrders(i);
            productRepository.save(product);
        }

        Map<String, Object> result = productService.getFeaturedProducts();

        @SuppressWarnings("unchecked")
        List<ProductDto> products = (List<ProductDto>) result.get("products");
        assertEquals(9, products.size());
        assertEquals("Product 9", products.get(0).getName());
    }

    @Test
    void whenAddCategory_thenCategoryIsSaved() {
        CategoryDto result = productService.addCategory("ROLE_ADMIN", new CategoryDto("New Category"));

        assertNotNull(result);
        assertEquals("New Category", result.getName());
        assertEquals(1, categoryRepository.count());
    }

    @Test
    void whenAddDuplicateCategory_thenThrowException() {
        categoryRepository.save(new Category("Existing Category"));

        assertThrows(ProductException.class, () ->
                productService.addCategory("ROLE_ADMIN", new CategoryDto("Existing Category")));
    }

    @Test
    void whenNonAdminTriesToAddProduct_thenThrowException() {
        String productJson = "{\"name\":\"Laptop\",\"price\":999.99,\"quantity\":10,\"description\":\"Powerful gaming laptop\",\"categories\":[\"Electronics\"]}";

        assertThrows(ProductException.class, () ->
                productService.addProduct("ROLE_USER", productJson, null));
    }

    @Test
    void whenAddProductWithInvalidData_thenThrowException() {
        String productJson = "{\"name\":\"\",\"price\":999.99,\"quantity\":-10,\"description\":\"Powerful gaming laptop\",\"categories\":[\"Electronics\"]}";

        assertThrows(IllegalArgumentException.class, () ->
                productService.addProduct("ROLE_ADMIN", productJson, null));
    }

    @Test
    void whenChangeProductDataWithInvalidId_thenThrowException() {
        Product product = new Product();
        product.setName("Original");
        product = productRepository.save(product);

        ProductDto updateDto = new ProductDto();
        updateDto.setId("different-id");
        updateDto.setName("Updated");

        Product finalProduct = product;
        assertThrows(ProductException.class, () ->
                productService.changeProductData("ROLE_ADMIN", finalProduct.getId(), updateDto));
    }

    @Test
    void whenSaveImage_thenSendKafkaMessage() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getContentType()).thenReturn("image/png");
        when(mockFile.getBytes()).thenReturn(new byte[10]);
        when(mockFile.getOriginalFilename()).thenReturn("test.png");

        productService.saveImage(mockFile);

        verify(kafkaTemplate).send(eq("file-uploads"), anyString(), any(FileUploadMessage.class));
    }

    @Test
    void whenHandleTotalPriceRequest_thenBlockProductsAndReturnPrice() {
        Product product = new Product();
        product.setAmountLeft(10);
        product.setPrice(BigDecimal.valueOf(100));
        String id = productRepository.save(product).getId();

        product.setAmountLeft(product.getAmountLeft() - 2);
        productRepository.save(product);

        Product updated = productRepository.findById(id).orElseThrow();
        assertEquals(8, updated.getAmountLeft());
    }

    @Test
    void whenChangeProductData_thenOnlySpecifiedFieldsAreUpdated() {
        Product product = new Product();
        product.setName("Original");
        product.setPrice(BigDecimal.valueOf(100));
        product.setAmountLeft(5);
        product = productRepository.save(product);

        ProductDto updateDto = new ProductDto();
        updateDto.setId(product.getId());
        updateDto.setName("Updated");
        updateDto.setPrice(BigDecimal.valueOf(200));
        updateDto.setQuantity(10);

        ProductDto result = productService.changeProductData("ROLE_ADMIN", product.getId(), updateDto);

        assertEquals("Updated", result.getName());
        assertEquals(200.0, result.getPrice().doubleValue());
        assertEquals(10, result.getQuantity());
    }

    @Test
    void whenGetProductsAsAdmin_thenReturnAllProductsIncludingUnavailable() {
        Product available = new Product();
        available.setName("Available");
        available.setPrice(BigDecimal.valueOf(100));
        available.setCategories(List.of(new Category("Electronics")));
        available.setRatings(Map.of(4, 2.8));
        available.setAmountLeft(4);
        available.setAvailable(true);
        Product unavailable = new Product();
        unavailable.setRatings(Map.of(4, 3.8));
        unavailable.setCategories(List.of(new Category("Electronics")));
        unavailable.setName("Unavailable");
        unavailable.setPrice(BigDecimal.valueOf(100));
        unavailable.setAmountLeft(4);
        unavailable.setAvailable(false);
        productRepository.saveAll(List.of(available, unavailable));

        ProductsListInfo adminResult = productService.getProducts(0, 10, "name", "asc", "", 0, 9999, List.of(), "ROLE_ADMIN");
        ProductsListInfo customerResult = productService.getProducts(0, 10, "name", "asc", "", 0, 9999, List.of(), "ROLE_CUSTOMER");

        assertEquals(2, adminResult.getProducts().size());
        assertEquals(1, customerResult.getProducts().size());
    }

    @Test
    void whenSaveImageWithInvalidType_thenThrowException() {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getContentType()).thenReturn("text/plain");
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");

        assertThrows(RuntimeException.class, () -> productService.saveImage(mockFile));
    }

    @Test
    void whenGetMaxPrice_thenReturnHighestPrice() {
        for (int i = 0; i < 5; i++) {
            Product product = new Product();
            product.setPrice(BigDecimal.valueOf(100 * (i + 1)));
            product.setAvailable(true);
            product.setAmountLeft(10);
            productRepository.save(product);
        }

        BigDecimal maxPrice = productService.getMaxPrice();

        assertEquals(500.0, maxPrice.doubleValue());
    }
}
