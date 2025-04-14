package com.shop.paymentservice.controller;

import com.shop.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

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
