package com.shop.customer.controller;

import com.shop.customer.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerContoller {

    private final CustomerService customerService;
}
