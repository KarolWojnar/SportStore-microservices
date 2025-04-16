package com.shop.orderservice.service;

import com.shop.orderservice.exception.OrderException;
import com.shop.orderservice.model.DeliveryTime;
import com.shop.orderservice.model.ProductInOrder;
import com.shop.orderservice.model.ShippingAddress;
import com.shop.orderservice.model.dto.*;
import com.shop.orderservice.model.entity.Order;
import com.shop.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final KafkaEventService kafkaEventService;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Transactional(rollbackFor = OrderException.class)
    public OrderDto getSummary(String userId) {
        try {
            log.info("Getting order summary for user: {}", userId);
            CustomerDto customerDto = kafkaEventService.optCustomer(userId).get(2, TimeUnit.SECONDS);
            String name = null;
            String lastName = null;
            ShippingAddress address = null;
            if (customerDto != null) {
                name = customerDto.getFirstName();
                lastName = customerDto.getLastName();
                address = customerDto.getShippingAddress();
            }

            Map<String, Integer> products = kafkaEventService
                    .getCartAndSetAsOrderProcessing(userId, true)
                    .get(5, TimeUnit.SECONDS);

            BigDecimal totalPrice = kafkaEventService
                    .getTotalPriceOfCart(products)
                    .get(5, TimeUnit.SECONDS);

            return OrderDto.builder()
                    .firstName(name)
                    .lastName(lastName)
                    .shippingAddress(address)
                    .totalPrice(totalPrice)
                    .deliveryTime(DeliveryTime.STANDARD)
                    .build();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new OrderException("Something went wrong during process order details.", e);
        }
    }

    public void cancelPayment(String userId) {
        try {
            Map<String, Integer> products = kafkaEventService
                    .getCartAndSetAsOrderProcessing(userId, false)
                    .get(5, TimeUnit.SECONDS);
            kafkaEventService.unlockProducts(products);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new OrderException("Something went wrong during cancel order.", e);
        }
    }

    public List<OrderBaseInfoDto> getUserOrders(String userId) {
        return orderRepository.findAllByUserId(userId).stream().map(OrderBaseInfoDto::mapToDto).toList();
    }

    public OrderDto getOrderById(String userId, String orderId, String email) {
        try {
            Order order = orderRepository.findByIdAndUserId(Long.valueOf(orderId), userId)
                    .orElseThrow(() -> new OrderException("Order not found."));
            CustomerDto customerDto = kafkaEventService.optCustomer(userId).join();
            List<String> productIds = order.getProducts().stream().map(ProductInOrder::getProductId).toList();
            List<ProductOrderDto> products = kafkaEventService.getProductsByIds(productIds)
                    .join();
            List<ProductInOrder> productsInOrder = order.getProducts();


            return orderMapper.mapToOrderDto(order, customerDto, email, products, productsInOrder);
        } catch (Exception e) {
            throw new OrderException("Something went wrong during get order details.", e);
        }
    }
}
