package com.shop.cartservice.service;

import com.shop.cartservice.exception.CartException;
import com.shop.cartservice.model.dto.ProductBase;
import com.shop.cartservice.model.dto.ProductCart;
import com.shop.cartservice.model.entity.Cart;
import com.shop.cartservice.repository.CartRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final KafkaEventService kafkaEventService;

    public void addToCart(String userId, String productId) {
        log.info("user: {}, product: {}", userId, productId);
        Cart cart = cartRepository.findById(userId);
        if (cart == null) {
            cart = new Cart(userId);
        }
        kafkaEventService.checkProductQuantity(
                userId,
                productId,
                (cart.getQuantity(productId) + 1)
        );
        cart.addProduct(productId, 1);
        log.info("cart: {}", cart.getProducts().size());
        cartRepository.save(userId, cart);
    }

    public void removeAllAmountOfProductFromCart(String id, @NonNull String userId) {
        cartRepository.removeItemFromCart(userId, id);
    }

    public void removeFromCart(String id, @NonNull String userId) {
        cartRepository.decreaseItemQuantity(userId, id);
    }

    public void deleteCart(@NonNull String userId) {
        log.info("Deleting cart for user: {}", userId);
        cartRepository.deleteById(userId);
    }

    public void validateCart(@NonNull String userId) {
        try {
            Cart cart = cartRepository.findById(userId);
            if (cart == null) {
                throw new CartException("Cart is empty.");
            }
            Map<String, Integer> productIds = new HashMap<>(cart.getProducts());
            kafkaEventService.validProductsInCart(productIds).get(5, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.error("Error validating cart", e);
            throw new CartException("Not enough products in stock.");
        }
    }

    public Map<String, Object> getCart(@NonNull String userId) {
        Cart cart = cartRepository.findById(userId);
        if (cart == null) {
            return Map.of("products", List.of());
        }
        try {
            List<String> productIds = new ArrayList<>(cart.getProducts().keySet());
            List<ProductBase> products = kafkaEventService.requestProductInfo(productIds)
                    .get(5, TimeUnit.SECONDS);
            List<ProductCart> productCarts = products.stream()
                    .map(product -> ProductCart.toDto(
                            product,
                            cart.getProducts().get(product.getProductId()))
                    ).toList();
            return Map.of("products", productCarts);
        } catch (Exception e) {
            log.error("Error getting cart products", e);
            throw new RuntimeException("Failed to retrieve cart information");
        }
    }
}
