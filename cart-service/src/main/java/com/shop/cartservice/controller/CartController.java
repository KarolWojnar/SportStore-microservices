package com.shop.cartservice.controller;

import com.shop.cartservice.service.CartService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<?> getCart(@RequestHeader("X-User-Id") @NonNull String userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestBody @NonNull String productId,
                                       @RequestHeader("X-User-Id") @NonNull String userId) {
        cartService.addToCart(userId, productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeFromCart(@RequestBody String id,
                                            @RequestHeader("X-User-Id") @NonNull String userId) {
        cartService.removeFromCart(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> removeAllAmountOfProductFromCart(@PathVariable String id,
                                                              @RequestHeader("X-User-Id") @NonNull String userId) {
        cartService.removeAllAmountOfProductFromCart(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<?> deleteCart(@RequestHeader("X-User-Id") @NonNull String userId) {
        cartService.deleteCart(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/valid")
    public ResponseEntity<?> validateCart(@RequestHeader("X-User-Id") @NonNull String userId) {
        cartService.validateCart(userId);
        return ResponseEntity.noContent().build();
    }
}
