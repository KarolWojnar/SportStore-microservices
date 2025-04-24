package com.shop.cartservice.repository;

import com.shop.cartservice.model.entity.Cart;

public interface CartRepository {
    Cart findById(String userId);
    void save(String userId, Cart cart);
    void deleteById(String userId);
    boolean existsById(String userId);
    void removeItemFromCart(String userId, String productId);
    void decreaseItemQuantity(String userId, String productId);

    void setItemQuantity(String userId, String productId, int quantity);
}
