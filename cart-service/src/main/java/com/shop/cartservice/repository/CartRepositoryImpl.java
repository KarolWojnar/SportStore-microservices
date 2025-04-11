package com.shop.cartservice.repository;

import com.shop.cartservice.model.entity.Cart;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CartRepositoryImpl implements CartRepository{

    private static final String CART_KEY_PREFIX = "cart:";
    private final RedisTemplate<String, Cart> redisTemplate;

    public void save(String userId, Cart cart) {
        redisTemplate.opsForValue().set(CART_KEY_PREFIX + userId, cart);
    }

    @Override
    public Cart findById(String userId) {
        String cartKey = CART_KEY_PREFIX + userId;
        return redisTemplate.opsForValue().get(cartKey);
    }


    @Override
    public void deleteById(String userId) {
        String cartKey = CART_KEY_PREFIX + userId;
        redisTemplate.delete(cartKey);
    }

    @Override
    public boolean existsById(String userId) {
        String cartKey = CART_KEY_PREFIX + userId;
        return redisTemplate.hasKey(cartKey);
    }

    @Override
    public void removeItemFromCart(String userId, String productId) {
        Cart cart = findById(userId);
        cart.getProducts().remove(productId);
        save(userId, cart);
    }

    @Override
    public void decreaseItemQuantity(String userId, String productId) {
        Cart cart = findById(userId);
        cart.removeProduct(productId);
        save(userId, cart);
    }

    @Override
    public void setItemQuantity(String userId, String productId, int quantity) {
        Cart cart = findById(userId);
        cart.setQuantity(productId, quantity);
        save(userId, cart);
    }
}
