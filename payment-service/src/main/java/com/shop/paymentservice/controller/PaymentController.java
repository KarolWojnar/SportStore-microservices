package com.shop.paymentservice.controller;

import com.shop.paymentservice.model.UrlPaymentResponse;
import com.shop.paymentservice.model.dto.OrderDto;
import com.shop.paymentservice.service.PaymentService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@RequestHeader("X-User-Id") @NonNull String userId,
                                           @RequestBody OrderDto orderDto) {
        return new ResponseEntity<>(new UrlPaymentResponse(paymentService.createPayment(orderDto, userId)), HttpStatus.CREATED);
    }

    @PostMapping("/repay")
    public ResponseEntity<?> createRepayment(@RequestHeader("X-User-Id") @NonNull String userId,
                                             @RequestHeader("X-User-Email") @NonNull String email,
                                             @RequestBody String orderId) {
        return ResponseEntity.ok(new UrlPaymentResponse(paymentService.createRepayment(orderId, userId, email)));
    }

    @Async
    @PostMapping("/webhook")
    public void webhook(@RequestBody String payload,
                        @RequestHeader("Stripe-Signature") String signature) {
        try {
            paymentService.webhook(payload, signature);
        } catch (Exception e) {
            log.info("Error during webhook: {}", e.getMessage());
        }
    }
}
