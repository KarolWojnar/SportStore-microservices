package com.shop.orderservice.controller;

import com.shop.orderservice.service.OrderService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/orders")
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestHeader("X-User-Id") @NonNull String userId) {
        return ResponseEntity.ok(orderService.getSummary(userId));
    }

    @DeleteMapping("/cancel")
    public ResponseEntity<?> cancelPayment(@RequestHeader("X-User-Id") @NonNull String userId) {
        orderService.cancelPayment(userId);
        return ResponseEntity.noContent().build();
    }
}
