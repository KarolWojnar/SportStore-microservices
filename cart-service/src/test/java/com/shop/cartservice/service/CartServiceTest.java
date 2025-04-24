package com.shop.cartservice.service;

import com.shop.cartservice.exception.CartException;
import com.shop.cartservice.model.dto.ProductBase;
import com.shop.cartservice.model.dto.ProductCart;
import com.shop.cartservice.model.entity.Cart;
import com.shop.cartservice.repository.CartRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private KafkaEventService kafkaEventService;

    @InjectMocks
    private CartService cartService;

    private final String userId = "user123";
    private final String productId = "product123";

    @Test
    void addToCart_WhenCartDoesNotExist_ShouldCreateNewCart() {
        when(cartRepository.findById(userId)).thenReturn(null);
        doNothing().when(kafkaEventService).checkProductQuantity(eq(userId), eq(productId), eq(1));

        cartService.addToCart(userId, productId);

        verify(cartRepository).findById(userId);
        verify(kafkaEventService).checkProductQuantity(eq(userId), eq(productId), eq(1));
        verify(cartRepository).save(eq(userId), any(Cart.class));
    }

    @Test
    void addToCart_WhenCartExists_ShouldAddProduct() {
        Cart existingCart = new Cart(userId);
        when(cartRepository.findById(userId)).thenReturn(existingCart);
        doNothing().when(kafkaEventService).checkProductQuantity(eq(userId), eq(productId), eq(1));

        cartService.addToCart(userId, productId);

        verify(cartRepository).findById(userId);
        verify(kafkaEventService).checkProductQuantity(eq(userId), eq(productId), eq(1));
        verify(cartRepository).save(eq(userId), any(Cart.class));
    }

    @Test
    void removeAllAmountOfProductFromCart_ShouldCallRepository() {
        cartService.removeAllAmountOfProductFromCart(productId, userId);

        verify(cartRepository).removeItemFromCart(userId, productId);
    }

    @Test
    void removeFromCart_ShouldCallRepository() {
        cartService.removeFromCart(productId, userId);

        verify(cartRepository).decreaseItemQuantity(userId, productId);
    }

    @Test
    void deleteCart_ShouldCallRepository() {
        cartService.deleteCart(userId);

        verify(cartRepository).deleteById(userId);
    }

    @Test
    void validateCart_WhenCartIsNull_ShouldThrowException() {
        when(cartRepository.findById(userId)).thenReturn(null);

        CartException exception = assertThrows(CartException.class, () -> cartService.validateCart(userId));
        assertEquals("Cart is empty.", exception.getMessage());
        verify(cartRepository).findById(userId);
        verifyNoInteractions(kafkaEventService);
    }

    @Test
    void validateCart_WhenKafkaServiceFails_ShouldThrowException() {
        Cart cart = new Cart(userId);
        cart.addProduct(productId, 2);
        when(cartRepository.findById(userId)).thenReturn(cart);

        CompletableFuture<Map<String, Integer>> future = new CompletableFuture<>();
        future.completeExceptionally(new ExecutionException("Failed", new RuntimeException()));
        when(kafkaEventService.validProductsInCart(anyMap())).thenReturn(future);

        CartException exception = assertThrows(CartException.class, () -> cartService.validateCart(userId));
        assertEquals("Not enough products in stock.", exception.getMessage());
        verify(cartRepository).findById(userId);
        verify(kafkaEventService).validProductsInCart(anyMap());
    }

    @Test
    void getCart_WhenCartIsNull_ShouldReturnEmptyProductsList() {
        when(cartRepository.findById(userId)).thenReturn(null);

        Map<String, Object> result = cartService.getCart(userId);

        assertEquals(List.of(), result.get("products"));
        verify(cartRepository).findById(userId);
        verifyNoInteractions(kafkaEventService);
    }

    @Test
    void getCart_WhenCartExists_ShouldReturnProducts() {
        Cart cart = new Cart(userId);
        cart.addProduct(productId, 2);
        when(cartRepository.findById(userId)).thenReturn(cart);

        ProductBase productBase = new ProductBase();
        productBase.setProductId(productId);
        productBase.setName("Test Product");
        productBase.setPrice(new BigDecimal("19.99"));

        CompletableFuture<List<ProductBase>> future = CompletableFuture.completedFuture(List.of(productBase));
        when(kafkaEventService.requestProductInfo(anyList())).thenReturn(future);

        Map<String, Object> result = cartService.getCart(userId);

        assertNotNull(result.get("products"));
        List<ProductCart> products = (List<ProductCart>) result.get("products");
        assertEquals(1, products.size());
        assertEquals(productId, products.get(0).getProductId());
        assertEquals(2, products.get(0).getQuantity());
        verify(cartRepository).findById(userId);
        verify(kafkaEventService).requestProductInfo(anyList());
    }

    @Test
    void getCart_WhenKafkaServiceFails_ShouldThrowException() {
        Cart cart = new Cart(userId);
        cart.addProduct(productId, 1);
        when(cartRepository.findById(userId)).thenReturn(cart);

        CompletableFuture<List<ProductBase>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Failed"));
        when(kafkaEventService.requestProductInfo(anyList())).thenReturn(future);

        Exception exception = assertThrows(RuntimeException.class, () -> cartService.getCart(userId));
        assertEquals("Failed to retrieve cart information", exception.getMessage());
        verify(cartRepository).findById(userId);
        verify(kafkaEventService).requestProductInfo(anyList());
    }
}