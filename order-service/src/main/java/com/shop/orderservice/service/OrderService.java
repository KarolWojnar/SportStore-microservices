package com.shop.orderservice.service;

import com.shop.orderservice.exception.OrderException;
import com.shop.orderservice.model.DeliveryTime;
import com.shop.orderservice.model.OrderStatus;
import com.shop.orderservice.model.ProductInOrder;
import com.shop.orderservice.model.ShippingAddress;
import com.shop.orderservice.model.dto.*;
import com.shop.orderservice.model.entity.Order;
import com.shop.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
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
    public static final Duration ORDER_CHANGE = Duration.ofDays(2);
    public static final Duration ORDER_DELETE = Duration.ofDays(1);
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
            kafkaEventService.unlockProducts(products).get(5, TimeUnit.SECONDS);
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

    public void cancelOrder(String id, String role, String userId) {
        try {
            Long orderId = Long.valueOf(id);
            Order order;
            if (role.equals("ROLE_ADMIN")) {
                order = orderRepository.findById(orderId).orElseThrow(() -> new OrderException("Order not found."));
            } else {
                order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(() -> new OrderException("Order not found."));
            }
            if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PROCESSING) {
                throw new OrderException("Order already paid.");
            }
            Map<String, Integer> products = order.getProducts().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            ProductInOrder::getProductId,
                            ProductInOrder::getAmount
                            ));
            kafkaEventService.unlockProducts(products).get(5, TimeUnit.SECONDS);
            order.setStatus(OrderStatus.ANNULLED);
            orderRepository.save(order);
        } catch (Exception e) {
            throw new OrderException("Something went wrong during cancel order.", e);
        }
    }

    public void refundOrder(String id, String userId) {
        Long orderId = Long.valueOf(id);
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(() -> new OrderException("Order not found."));
        Instant fourteenDaysAgo = Instant.now().minus(14, ChronoUnit.DAYS);
        Date refundDeadline = Date.from(fourteenDaysAgo);
        if (order.getOrderDate().before(refundDeadline)) {
            throw new OrderException("Order date is more than 14 days ago.");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new OrderException("Order not delivered.");
        }
        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);
    }

    @Scheduled(cron = "*/5 * * * * *")
    public void changeOrderStatus() {
        Date minusTwoDays = new Date(System.currentTimeMillis() - ORDER_CHANGE.toMillis());
        List<Order> orders = orderRepository
                .findAllByStatusIsNotAndLastModifiedBefore(OrderStatus.CREATED, minusTwoDays);
        for (Order order : orders) {
            order.setNextStatus();
            if (order.getStatus() == OrderStatus.DELIVERED && !order.isEmailSent()) {
                kafkaEventService.sendOrderDeliveredEmail(order);
                order.setEmailSent(true);
            }
        }
        log.info("Changed {} orders status. date: {}", orders.size(),
                Date.from(Instant.now()));
        orderRepository.saveAll(orders);
    }

    @Scheduled(cron = "0 */15 * * * *")
    public void deleteNotPaidOrders() {
        Date minusDays = new Date(System.currentTimeMillis() - ORDER_DELETE.toMillis());
        List<Order> orders = orderRepository.findAllByStatusAndOrderDateBefore(OrderStatus.CREATED, minusDays);
        handleNotPaidOrders(orders);
        log.info("Deleted {} orders. date: {}", orders.size(),
                Date.from(Instant.now()));
    }

    public void handleNotPaidOrders(List<Order> orders) {
        for (Order order : orders) {
            try {
            Map<String, Integer> products = order.getProducts().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            ProductInOrder::getProductId,
                            ProductInOrder::getAmount
                    ));
            kafkaEventService.unlockProducts(products).get(5, TimeUnit.SECONDS);
            orderRepository.delete(order);
            } catch (Exception e) {
                throw new OrderException("Something went wrong during delete order.", e);
            }
        }

    }

    public List<OrderBaseDto> getOrders(String role, int page, int size, String status) {
        if (!role.equals("ROLE_ADMIN")) {
            throw new OrderException("You are not admin.");
        }
        Pageable pageable = PageRequest.of(page, size);
        if (status == null || status.isEmpty()) {
            return orderRepository.findAll(pageable).map(OrderBaseDto::mapToDto).toList();
        }
        return orderRepository.findAllByStatus(OrderStatus.valueOf(status), pageable).stream().map(OrderBaseDto::mapToDto).toList();
    }
}
