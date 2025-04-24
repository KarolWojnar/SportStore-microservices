package com.shop.customer.controller;

import com.shop.customer.model.dto.CustomerDto;
import com.shop.customer.service.CustomerService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerContoller {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<?> getCustomer(@RequestHeader("X-User-Id") @NonNull String userId) {
        return ResponseEntity.ok(customerService.getUserDetails(userId));
    }

    @PutMapping
    public ResponseEntity<?> updateCustomer(@RequestBody CustomerDto customerDto,
                                            @RequestHeader("X-User-Id") @NonNull String userId) {
        return ResponseEntity.ok(customerService.updateCustomer(customerDto, userId));
    }
}
