package com.shop.cartservice.service;

import com.shop.cartservice.exception.CartException;
import com.shop.cartservice.model.dto.*;
import com.shop.cartservice.model.entity.Cart;
import com.shop.cartservice.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private KafkaEventService kafkaEventService;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    @Captor
    private ArgumentCaptor<Object> messageCaptor;

    private final String userId = "user123";
    private final String productId = "product123";
    private final String correlationId = "corr123";

    @Test
    void checkProductQuantity_ShouldSendKafkaMessage() {
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        kafkaEventService.checkProductQuantity(userId, productId, 2);

        verify(kafkaTemplate).send(topicCaptor.capture(), messageCaptor.capture());
        assertEquals("product-cart-quantity-check-request", topicCaptor.getValue());
        ProductQuantityCheck sentMessage = (ProductQuantityCheck) messageCaptor.getValue();
        assertEquals(userId, sentMessage.getUserId());
        assertEquals(productId, sentMessage.getProductId());
        assertEquals(2, sentMessage.getQuantity());
    }

    @Test
    void returnNewValueOfCartQuantity_ShouldUpdateCart() {
        ProductQuantityCheck checkResponse = new ProductQuantityCheck(userId, productId, 3);

        kafkaEventService.returnNewValueOfCartQuantity(checkResponse);

        verify(cartRepository).setItemQuantity(userId, productId, 3);
    }

    @Test
    void requestProductInfo_ShouldSendKafkaMessageAndReturnFuture() {
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);
        List<String> productIds = List.of(productId);

        CompletableFuture<List<ProductBase>> future = kafkaEventService.requestProductInfo(productIds);

        assertNotNull(future);
        verify(kafkaTemplate).send(topicCaptor.capture(), messageCaptor.capture());
        assertEquals("product-cart-info-request", topicCaptor.getValue());
        ProductInfoRequest sentRequest = (ProductInfoRequest) messageCaptor.getValue();
        assertEquals(productIds, sentRequest.getProductIds());
        assertNotNull(sentRequest.getCorrelationId());
    }

    @Test
    void handleProductInfoResponse_ShouldCompleteCorrespondingFuture() throws Exception {
        List<String> productIds = List.of(productId);
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        CompletableFuture<List<ProductBase>> future = kafkaEventService.requestProductInfo(productIds);

        verify(kafkaTemplate).send(anyString(), messageCaptor.capture());
        ProductInfoRequest sentRequest = (ProductInfoRequest) messageCaptor.getValue();
        String capturedCorrelationId = sentRequest.getCorrelationId();

        ProductBase product = new ProductBase();
        product.setProductId(productId);
        ProductInfoResponse response = new ProductInfoResponse();
        response.setCorrelationId(capturedCorrelationId);
        response.setProducts(List.of(product));

        kafkaEventService.handleProductInfoResponse(response);

        assertTrue(future.isDone());
        List<ProductBase> result = future.get(1, TimeUnit.SECONDS);
        assertEquals(1, result.size());
        assertEquals(productId, result.get(0).getProductId());
    }

    @Test
    void checkCartHasItems_WhenCartExists_ShouldRespondWithTrue() {
        when(cartRepository.existsById(userId)).thenReturn(true);
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);
        CartInfoRequest request = new CartInfoRequest(correlationId, userId);

        kafkaEventService.checkCartHasItems(request);

        verify(cartRepository).existsById(userId);
        verify(kafkaTemplate).send(topicCaptor.capture(), messageCaptor.capture());
        assertEquals("cart-items-response", topicCaptor.getValue());
        CartInfoResponse sentResponse = (CartInfoResponse) messageCaptor.getValue();
        assertEquals(correlationId, sentResponse.getCorrelationId());
        assertTrue(sentResponse.isCartHasItems());
    }

    @Test
    void validProductsInCart_ShouldSendKafkaMessageAndReturnFuture() {
        Map<String, Integer> products = Map.of(productId, 2);
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        CompletableFuture<Map<String, Integer>> future = kafkaEventService.validProductsInCart(products);

        assertNotNull(future);
        verify(kafkaTemplate).send(topicCaptor.capture(), messageCaptor.capture());
        assertEquals("cart-validation-request", topicCaptor.getValue());
        CartValidationRequest sentRequest = (CartValidationRequest) messageCaptor.getValue();
        assertEquals(products, sentRequest.getProducts());
        assertNotNull(sentRequest.getCorrelationId());
    }

    @Test
    void handleValidationResponse_WhenSuccessful_ShouldCompleteCorrespondingFuture() throws Exception {
        Map<String, Integer> products = Map.of(productId, 2);
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        CompletableFuture<Map<String, Integer>> future = kafkaEventService.validProductsInCart(products);

        verify(kafkaTemplate).send(anyString(), messageCaptor.capture());
        CartValidationRequest sentRequest = (CartValidationRequest) messageCaptor.getValue();
        String capturedCorrelationId = sentRequest.getCorrelationId();

        CartValidationResponse response = new CartValidationResponse();
        response.setCorrelationId(capturedCorrelationId);
        response.setProducts(products);
        response.setError(null);

        kafkaEventService.handleValidationResponse(response);

        assertTrue(future.isDone());
        Map<String, Integer> result = future.get(1, TimeUnit.SECONDS);
        assertEquals(products, result);
    }

    @Test
    void handleValidationResponse_WhenError_ShouldCompleteExceptionally() {
        Map<String, Integer> products = Map.of(productId, 2);
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        CompletableFuture<Map<String, Integer>> future = kafkaEventService.validProductsInCart(products);

        verify(kafkaTemplate).send(anyString(), messageCaptor.capture());
        CartValidationRequest sentRequest = (CartValidationRequest) messageCaptor.getValue();
        String capturedCorrelationId = sentRequest.getCorrelationId();

        CartValidationResponse response = new CartValidationResponse();
        response.setCorrelationId(capturedCorrelationId);
        response.setError("Not enough stock");

        kafkaEventService.handleValidationResponse(response);

        assertTrue(future.isCompletedExceptionally());
        ExecutionException exception = assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.SECONDS));
        assertInstanceOf(CartException.class, exception.getCause());
        assertEquals("Not enough stock", exception.getCause().getMessage());
    }

    @Test
    void getProductsAndBlockCart_WhenCartExists_ShouldSendResponse() {
        Cart cart = new Cart(userId);
        cart.addProduct(productId, 2);
        when(cartRepository.findById(userId)).thenReturn(cart);
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        ProductsInCartInfoRequest request = new ProductsInCartInfoRequest();
        request.setUserId(userId);
        request.setCorrelationId(correlationId);
        request.setBlockCart(true);

        kafkaEventService.getProductsAndBlockCart(request);

        verify(cartRepository).findById(userId);
        verify(cartRepository).save(eq(userId), any(Cart.class));
        verify(kafkaTemplate).send(topicCaptor.capture(), messageCaptor.capture());
        assertEquals("cart-product-block-response", topicCaptor.getValue());
        ProductsInCartInfoResponse sentResponse = (ProductsInCartInfoResponse) messageCaptor.getValue();
        assertEquals(correlationId, sentResponse.getCorrelationId());
        assertEquals(1, sentResponse.getProduct().size());
        assertTrue(sentResponse.getProduct().containsKey(productId));
    }

    @Test
    void getProductsAndBlockCart_WhenCartIsNull_ShouldThrowException() {
        when(cartRepository.findById(userId)).thenReturn(null);

        ProductsInCartInfoRequest request = new ProductsInCartInfoRequest();
        request.setUserId(userId);
        request.setCorrelationId(correlationId);

        CartException exception = assertThrows(CartException.class,
                () -> kafkaEventService.getProductsAndBlockCart(request));
        assertEquals("Cart is empty.", exception.getMessage());
        verify(cartRepository).findById(userId);
        verifyNoMoreInteractions(cartRepository);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void getProducts_WhenCartExists_ShouldSendResponse() {
        Cart cart = new Cart(userId);
        cart.addProduct(productId, 2);
        when(cartRepository.findById(userId)).thenReturn(cart);
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        ProductsInCartInfoRequest request = new ProductsInCartInfoRequest();
        request.setUserId(userId);
        request.setCorrelationId(correlationId);

        kafkaEventService.getProducts(request);

        verify(cartRepository).findById(userId);
        verify(kafkaTemplate).send(topicCaptor.capture(), messageCaptor.capture());
        assertEquals("cart-product-payment-response", topicCaptor.getValue());
        ProductsInCartInfoResponse sentResponse = (ProductsInCartInfoResponse) messageCaptor.getValue();
        assertEquals(correlationId, sentResponse.getCorrelationId());
        assertEquals(1, sentResponse.getProduct().size());
    }

    @Test
    void getProducts_WhenCartIsNull_ShouldThrowException() {
        when(cartRepository.findById(userId)).thenReturn(null);

        ProductsInCartInfoRequest request = new ProductsInCartInfoRequest();
        request.setUserId(userId);
        request.setCorrelationId(correlationId);

        CartException exception = assertThrows(CartException.class,
                () -> kafkaEventService.getProducts(request));
        assertEquals("Cart is empty.", exception.getMessage());
        verify(cartRepository).findById(userId);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void deleteCart_ShouldDeleteAndSendResponse() {
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        ProductsInCartInfoRequest request = new ProductsInCartInfoRequest();
        request.setUserId(userId);
        request.setCorrelationId(correlationId);

        kafkaEventService.deleteCart(request);

        verify(cartRepository).deleteById(userId);
        verify(kafkaTemplate).send(eq("cart-delete-response"), eq(correlationId));
    }
}