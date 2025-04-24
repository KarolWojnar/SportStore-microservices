package com.shop.cartservice.repository;

import com.shop.cartservice.model.entity.Cart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartRepositoryImplTest {

    private static final String USER_ID = "user123";
    private static final String PRODUCT_ID = "prod456";
    private static final String CART_KEY = "cart:user123";

    @Mock
    private RedisTemplate<String, Cart> redisTemplate;

    @Mock
    private ValueOperations<String, Cart> valueOperations;

    @InjectMocks
    private CartRepositoryImpl cartRepository;

    private Cart testCart;

    @BeforeEach
    void setUp() {
        Map<String, Integer> products = new HashMap<>();
        products.put(PRODUCT_ID, 2);
        testCart = new Cart(USER_ID, products);
    }

    @Test
    void save_shouldStoreCartInRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cartRepository.save(USER_ID, testCart);

        verify(valueOperations).set(CART_KEY, testCart);
    }

    @Test
    void findById_shouldReturnCartWhenExists() {
        when(valueOperations.get(CART_KEY)).thenReturn(testCart);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Cart result = cartRepository.findById(USER_ID);

        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());
        assertEquals(1, result.getProducts().size());
        verify(valueOperations).get(CART_KEY);
    }

    @Test
    void findById_shouldReturnNullWhenNotExists() {
        when(valueOperations.get(CART_KEY)).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Cart result = cartRepository.findById(USER_ID);

        assertNull(result);
        verify(valueOperations).get(CART_KEY);
    }

    @Test
    void deleteById_shouldDeleteCartFromRedis() {
        cartRepository.deleteById(USER_ID);

        verify(redisTemplate).delete(CART_KEY);
    }

    @Test
    void existsById_shouldReturnTrueWhenCartExists() {
        when(redisTemplate.hasKey(CART_KEY)).thenReturn(true);

        boolean exists = cartRepository.existsById(USER_ID);

        assertTrue(exists);
        verify(redisTemplate).hasKey(CART_KEY);
    }

    @Test
    void existsById_shouldReturnFalseWhenCartNotExists() {
        when(redisTemplate.hasKey(CART_KEY)).thenReturn(false);

        boolean exists = cartRepository.existsById(USER_ID);

        assertFalse(exists);
        verify(redisTemplate).hasKey(CART_KEY);
    }

    @Test
    void removeItemFromCart_shouldRemoveProductAndSaveCart() {
        when(valueOperations.get(CART_KEY)).thenReturn(testCart);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cartRepository.removeItemFromCart(USER_ID, PRODUCT_ID);

        assertFalse(testCart.getProducts().containsKey(PRODUCT_ID));
        verify(valueOperations).set(CART_KEY, testCart);
    }

    @Test
    void decreaseItemQuantity_shouldDecreaseQuantityAndSaveCart() {
        when(valueOperations.get(CART_KEY)).thenReturn(testCart);
        int initialQuantity = testCart.getProducts().get(PRODUCT_ID);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cartRepository.decreaseItemQuantity(USER_ID, PRODUCT_ID);

        assertEquals(initialQuantity - 1, testCart.getProducts().get(PRODUCT_ID));
        verify(valueOperations).set(CART_KEY, testCart);
    }

    @Test
    void setItemQuantity_shouldUpdateQuantityAndSaveCart() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        int newQuantity = 5;
        when(valueOperations.get(CART_KEY)).thenReturn(testCart);

        cartRepository.setItemQuantity(USER_ID, PRODUCT_ID, newQuantity);

        assertEquals(newQuantity, testCart.getProducts().get(PRODUCT_ID));
        verify(valueOperations).set(CART_KEY, testCart);
    }
}