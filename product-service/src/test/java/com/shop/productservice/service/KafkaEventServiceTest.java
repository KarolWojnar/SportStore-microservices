package com.shop.productservice.service;

import com.shop.productservice.model.dto.*;
import com.shop.productservice.model.entity.Product;
import com.shop.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaEventService kafkaEventService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId("1");
        testProduct.setName("Test Product");
        testProduct.setPrice(BigDecimal.TEN);
        testProduct.setAmountLeft(10);
        testProduct.setOrders(0);
    }

    @Test
    void checkProductAmountAvailability_ShouldSendAvailableQuantity() {
        ProductQuantityCheck request = new ProductQuantityCheck("user1", "1", 5);
        when(productRepository.findById("1")).thenReturn(Optional.of(testProduct));

        kafkaEventService.checkProductAmountAvaibility(request);

        verify(kafkaTemplate).send(eq("product-cart-quantity-check-response"), any(ProductQuantityCheck.class));
    }

    @Test
    void handleProductInfoRequest_ShouldSendProductInfoResponse() {
        ProductInfoRequest request = new ProductInfoRequest("corr1", List.of("1"));
        when(productRepository.findAllById(List.of("1"))).thenReturn(List.of(testProduct));

        kafkaEventService.handleProductInfoRequest(request);

        verify(kafkaTemplate).send(eq("product-cart-info-response"), any(ProductInfoResponse.class));
    }

    @Test
    void handleValidationRequest_ShouldSendSuccessResponse_WhenAllProductsAvailable() {
        CartValidationRequest request = new CartValidationRequest("corr1", Map.of("1", 5));
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(testProduct));

        kafkaEventService.handleValidationRequest(request);

        verify(kafkaTemplate).send(eq("cart-validation-response"), any(CartValidationResponse.class));
    }

    @Test
    void handleValidationRequest_ShouldSendErrorResponse_WhenInsufficientStock() {
        testProduct.setAmountLeft(1);
        CartValidationRequest request = new CartValidationRequest("corr1", Map.of("1", 5));
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(testProduct));

        kafkaEventService.handleValidationRequest(request);

        verify(kafkaTemplate).send(eq("cart-validation-response"), argThat(response ->
                ((CartValidationResponse)response).getError() != null
        ));
    }

    @Test
    void getTotalPriceAndBlockProductsRequest_ShouldCalculateTotalAndBlockStock() {
        TotalPriceOfProductsRequest request = new TotalPriceOfProductsRequest("corr1", Map.of("1", 2));
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(testProduct));

        kafkaEventService.getTotalPriceAndBlockProductsRequest(request);

        verify(kafkaTemplate).send(eq("total-price-response"), any(TotalPriceOfProductsResponse.class));
        verify(productRepository).saveAll(anyList());
    }

    @Test
    void getTotalPriceAndBlockProductsRequest_ShouldSendError_WhenStockInsufficient() {
        testProduct.setAmountLeft(1);
        TotalPriceOfProductsRequest request = new TotalPriceOfProductsRequest("corr1", Map.of("1", 5));
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(testProduct));

        kafkaEventService.getTotalPriceAndBlockProductsRequest(request);

        verify(kafkaTemplate).send(eq("total-price-response"), argThat(response ->
                ((TotalPriceOfProductsResponse)response).getErrorMessage() != null
        ));
    }

    @Test
    void getTotalPriceRequest_ShouldSendTotalPriceWithoutBlockingStock() {
        TotalPriceOfProductsRequest request = new TotalPriceOfProductsRequest("corr1", Map.of("1", 3));
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(testProduct));

        kafkaEventService.getTotalPriceRequest(request);

        verify(kafkaTemplate).send(eq("total-price-payment-response"), any(TotalPriceOfProductsResponse.class));
    }

    @Test
    void getProductsByIdsRequest_ShouldRespondWithMappedProducts() {
        ProductsByIdRequest request = new ProductsByIdRequest("corr1", List.of("1"));
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(testProduct));

        kafkaEventService.getProductsByIdsRequest(request);

        verify(kafkaTemplate).send(eq("products-by-id-response"), any(ProductsByIdResponse.class));
    }

    @Test
    void incrementSoldItems_ShouldUpdateProductOrders() {
        Map<String, Integer> request = Map.of("1", 3);
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(testProduct));

        kafkaEventService.incrementSoldItems(request);

        assertEquals(3, testProduct.getOrders());
        verify(productRepository).saveAll(anyList());
    }

    @Test
    void setOrderProductAsRated_ShouldSendRequestAndReturnFuture() {
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        CompletableFuture<OrderProductRatedRequest> future =
                kafkaEventService.setOrderProductAsRated("order1", "product1");

        assertNotNull(future);
        verify(kafkaTemplate).send(eq("order-product-rated-request"), any(OrderProductRatedRequest.class));
    }

    @Test
    void handleOrderProductRatedResponse_ShouldCompleteFuture() {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<OrderProductRatedRequest> future = new CompletableFuture<>();
        kafkaEventService.orderProductAsRatedMap.put(correlationId, future);

        kafkaEventService.handleOrderProductRatedResponse(correlationId);

        assertTrue(future.isDone());
    }
}