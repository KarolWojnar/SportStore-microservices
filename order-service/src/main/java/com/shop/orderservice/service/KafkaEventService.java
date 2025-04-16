package com.shop.orderservice.service;

import com.shop.orderservice.exception.OrderException;
import com.shop.orderservice.model.OrderStatus;
import com.shop.orderservice.model.ProductInOrder;
import com.shop.orderservice.model.dto.*;
import com.shop.orderservice.model.entity.Order;
import com.shop.orderservice.repository.OrderRepository;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventService {

    private final OrderRepository orderRepository;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<String, CompletableFuture<CustomerDto>> requestsForCustomer = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Map<String, Integer>>> requestsForCart = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<BigDecimal>> requestsForTotalPrice = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Map<String, BigDecimal>>> requestsForTotalPriceItem = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Void>> requestsForUnlockProducts = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<List<ProductOrderDto>>> requestsForProducts = new ConcurrentHashMap<>();

    public CompletableFuture<CustomerDto> optCustomer(String userId) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<CustomerDto> future = new CompletableFuture<>();
        requestsForCustomer.put(correlationId, future);
        try {
            CustomerInfoRequest request = new CustomerInfoRequest(correlationId, userId);
            kafkaTemplate.send("customer-info-request", request);
            ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                requestsForCustomer.remove(correlationId);
                future.completeExceptionally(new RuntimeException("Timeout waiting for customer info"));
            }, 5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            requestsForCustomer.remove(correlationId);
            future.completeExceptionally(e);
        }
        return future;
    }

    @KafkaListener(topics = "customer-info-response", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void customerInfoResponse(CustomerInfoResponse customerInfo) {
        CompletableFuture<CustomerDto> future = requestsForCustomer.remove(customerInfo.getCorrelationId());
        if (future != null) {
            future.complete(customerInfo.getCustomer());
        }
    }

    public CompletableFuture<Map<String, Integer>> getCartAndSetAsOrderProcessing(String userId, boolean isOrderProcessing) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<Map<String, Integer>> future = new CompletableFuture<>();
        requestsForCart.put(correlationId, future);
        try {
            kafkaTemplate.send(
                    "cart-product-block-request",
                    new ProductsInCartInfoRequest(correlationId, isOrderProcessing, userId));

            ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                requestsForCart.remove(correlationId);
                future.completeExceptionally(new RuntimeException("Timeout waiting for cart"));
            }, 5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            requestsForCart.remove(correlationId);
            future.completeExceptionally(e);
        }
        return future;
    }

    @KafkaListener(topics = "cart-product-block-response", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void cartResponse(ProductsInCartInfoResponse response) {
        CompletableFuture<Map<String, Integer>> future = requestsForCart.remove(response.getCorrelationId());
        if (future != null) {
            future.complete(response.getProduct());
        }
    }

    public CompletableFuture<BigDecimal> getTotalPriceOfCart(Map<String, Integer> products) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<BigDecimal> future = new CompletableFuture<>();
        requestsForTotalPrice.put(correlationId, future);
        try {
            TotalPriceOfProductsRequest request = new TotalPriceOfProductsRequest(correlationId, products);
            kafkaTemplate.send("total-price-request", request);
            ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                requestsForTotalPrice.remove(correlationId);
                future.completeExceptionally(new RuntimeException("Timeout waiting for total price"));
            }, 5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            requestsForTotalPrice.remove(correlationId);
            future.completeExceptionally(e);
        }
        return future;
    }

    @KafkaListener(topics = "total-price-response", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void totalPriceResponse(TotalPriceOfProductsResponse response) {
        CompletableFuture<BigDecimal> future = requestsForTotalPrice.remove(response.getCorrelationId());
        if (response.getErrorMessage() != null) {
            future.completeExceptionally(new OrderException(response.getErrorMessage()));
            return;
        }
        if (future != null) {
            future.complete(response.getTotalPrice());
        }
    }

    public CompletableFuture<Void> unlockProducts(Map<String, Integer> products) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<Void> future = new CompletableFuture<>();
        requestsForUnlockProducts.put(correlationId, future);
        try {
            TotalPriceOfProductsRequest request = new TotalPriceOfProductsRequest(correlationId, products);
            kafkaTemplate.send("order-product-unlock-request", request);
            ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                requestsForUnlockProducts.remove(correlationId);
                future.completeExceptionally(new RuntimeException("Timeout waiting for total price"));
            }, 5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            requestsForUnlockProducts.remove(correlationId);
            future.completeExceptionally(e);
        }
        return future;
    }

    @KafkaListener(topics = "order-product-unlock-response", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void unlockProductsResponse(TotalPriceOfProductsResponse response) {
        CompletableFuture<Void> future = requestsForUnlockProducts.remove(response.getCorrelationId());
        if (response.getErrorMessage() != null) {
            future.completeExceptionally(new OrderException(response.getErrorMessage()));
            return;
        }
        if (future != null) {
            future.complete(null);
        }
    }

    @KafkaListener(topics = "order-create-request", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void orderCreateResponse(CreateOrderRequest request) {
        try {
            String orderId = createOrder(request.getOrderBaseInfo());
            kafkaTemplate.send("order-create-response", new CreateOrderResponse(request.getCorrelationId(), orderId));
        } catch (Exception e) {
            kafkaTemplate.send("order-create-response", new CreateOrderResponse(request.getCorrelationId(), null));
        }
    }

    private String createOrder(OrderBaseInfo orderBaseInfo) {
        try {
            Map<String, BigDecimal> products = getPriceOfProducts(orderBaseInfo.getProducts().keySet())
                    .get(5, TimeUnit.SECONDS);
            List<ProductInOrder> productInOrder = orderBaseInfo.getProducts().entrySet().stream()
                    .map(entry -> {
                        BigDecimal price = products.get(entry.getKey());
                        return new ProductInOrder(
                                entry.getKey(),
                                entry.getValue(),
                                price
                        );
                    }).collect(Collectors.toList());

            Order order = orderRepository.save(new Order(
                    productInOrder,
                    orderBaseInfo.getUserId(),
                    orderBaseInfo.getShippingAddress(),
                    orderBaseInfo.getTotalPrice().divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP),
                    orderBaseInfo.getPaymentMethod().name()
                    )
            );
            return String.valueOf(order.getId());
        } catch (Exception e) {
            throw new OrderException(e.getMessage());
        }
    }

    private CompletableFuture<Map<String, BigDecimal>> getPriceOfProducts(Set<String> productIds) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<Map<String, BigDecimal>> future = new CompletableFuture<>();
        requestsForTotalPriceItem.put(correlationId, future);
        try {
            ProductPriceByIdRequest request = new ProductPriceByIdRequest(correlationId, productIds);
            kafkaTemplate.send("products-total-price-by-id-request", request);
            ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                requestsForTotalPriceItem.remove(correlationId);
                future.completeExceptionally(new RuntimeException("Timeout waiting for total price"));
            }, 5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            requestsForTotalPriceItem.remove(correlationId);
            future.completeExceptionally(e);
        }
        return future;
    }

    @KafkaListener(topics = "products-total-price-by-id-response", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void totalPriceResponse(ProductPriceByIdResponse response) {
        CompletableFuture<Map<String, BigDecimal>> future = requestsForTotalPriceItem.remove(response.getCorrelationId());
        if (response.getErrorMessage() != null) {
            future.completeExceptionally(new OrderException(response.getErrorMessage()));
            return;
        }
        if (future != null) {
            future.complete(response.getProductPriceDto());
        }
    }

    @KafkaListener(topics = "order-session-request", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void setSessionIdForOrder(OrderSessionRequest request) {
        log.info("recived session id.");
        Order order = orderRepository.findById(Long.valueOf(request.getOrderId())).orElse(null);
        if (order == null) {
            kafkaTemplate.send("order-session-response", request.getOrderId());
            return;
        }
        log.info("recived session id.");
        order.setSessionId(request.getSessionId());
        orderRepository.save(order);
        kafkaTemplate.send("order-session-response", request.getCorrelationId());
    }

    @KafkaListener(topics = "order-paid", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void orderSessionIdAndSetAsProcessing(String sessionId) {
        orderRepository.findBySessionId(sessionId).ifPresent(order -> {
            order.setNewStatus(OrderStatus.PROCESSING);
            orderRepository.save(order);
        });
    }

    @KafkaListener(topics = "order-info-request", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void orderInfoResponse(OrderRepaymentRequest request) {
        Order order = orderRepository.findById(Long.valueOf(request.getOrderId())).orElse(null);
        if (order == null) {
            OrderRepaymentResponse response = new OrderRepaymentResponse(request.getCorrelationId(), "Order not found.", null);
            kafkaTemplate.send("order-info-response", response);
            return;
        }
        OrderInfoRepayment orderDto = new OrderInfoRepayment();
        orderDto.setTotalPrice(order.getTotalPrice());
        orderDto.setPaymentMethod(SessionCreateParams.PaymentMethodType.valueOf(order.getPaymentMethod()));
        OrderRepaymentResponse response = new OrderRepaymentResponse(
                request.getCorrelationId(),
                null,
                orderDto
                );
        kafkaTemplate.send("order-info-response", response);
    }


    public CompletableFuture<List<ProductOrderDto>> getProductsByIds(List<String> productIds) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<List<ProductOrderDto>> future = new CompletableFuture<>();
        requestsForProducts.put(correlationId, future);
        try {
            ProductsByIdRequest request = new ProductsByIdRequest(correlationId, productIds);
            kafkaTemplate.send("products-by-id-request", request);
            ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                requestsForProducts.remove(correlationId);
                future.completeExceptionally(new RuntimeException("Timeout waiting for products"));
            }, 5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            requestsForProducts.remove(correlationId);
            future.completeExceptionally(e);
        }
        return future;
    }

    @KafkaListener(topics = "products-by-id-response", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void productsByIdResponse(ProductsByIdResponse response) {
        CompletableFuture<List<ProductOrderDto>> future = requestsForProducts.remove(response.getCorrelationId());
        if (response.getErrorMessage() != null) {
            future.completeExceptionally(new OrderException(response.getErrorMessage()));
            return;
        }
        if (future != null) {
            future.complete(response.getProductDto());
        }
    }
}
