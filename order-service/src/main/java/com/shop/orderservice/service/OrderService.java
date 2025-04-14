package com.shop.orderservice.service;

import com.shop.orderservice.exception.OrderException;
import com.shop.orderservice.model.DeliveryTime;
import com.shop.orderservice.model.ShippingAddress;
import com.shop.orderservice.model.dto.OrderDto;
import com.shop.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaEventService kafkaEventService;

    @Transactional(rollbackFor = OrderException.class)
    public OrderDto getSummary(String userId) {

        //todo: make all valid
//        kafkaEventService.validProductsInCart(productIds).get(5, TimeUnit.SECONDS);

//        BigDecimal totalPrice = kafkaEventService.getTotalPriceofCart(userId);

        return OrderDto.builder()
                .firstName("name")
                .lastName("lastName")
                .shippingAddress(new ShippingAddress())
                .totalPrice(new BigDecimal("111"))
                .deliveryTime(DeliveryTime.STANDARD)
                .build();
    }
}
