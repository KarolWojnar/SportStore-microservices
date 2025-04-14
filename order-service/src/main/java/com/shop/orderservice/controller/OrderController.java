package com.shop.orderservice.controller;

import com.shop.orderservice.service.OrderService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSummary(@RequestHeader("X-User-Id") @NonNull String userId) {
        return ResponseEntity.ok(orderService.getSummary(userId));
    }
}
