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
import org.hibernate.Hibernate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;
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
    private final Map<String, CompletableFuture<String>> requestsForEmails = new ConcurrentHashMap<>();

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

    @Transactional
    @KafkaListener(topics = "order-paid", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void orderSessionIdAndSetAsProcessing(String sessionId) {
        try {
            orderRepository.findBySessionId(sessionId).ifPresent(order -> {
                OrderSentEmailDto orderSentEmailDto = findAllOrderData(order);
                order.setNewStatus(OrderStatus.PROCESSING);
                orderRepository.save(order);
                kafkaTemplate.send("order-sent-request", orderSentEmailDto);
            });
        } catch (Exception e) {
            throw new OrderException("Something went wrong during setting order status.", e);
        }
    }

    public void sendOrderDeliveredEmail(Order order) {
        try {
            Hibernate.initialize(order.getProducts());
            OrderSentEmailDto orderSentEmailDto = findAllOrderData(order);
            List<String> productIds = order.getProducts().stream().map(ProductInOrder::getProductId).toList();
            List<ProductOrderDto> products = getProductsByIds(productIds).get(5, TimeUnit.SECONDS);
            List<ProductInfoEmail> productInfoEmails = products.stream().map(product -> {
                ProductInfoEmail productInfoEmail = new ProductInfoEmail();
                productInfoEmail.setName(product.getName());
                productInfoEmail.setAmount(order.getProducts().stream()
                        .filter(p -> p.getProductId().equals(product.getId()))
                        .findFirst()
                        .map(ProductInOrder::getAmount)
                        .orElse(0));
                productInfoEmail.setPrice(order.getProducts().stream()
                        .filter(p -> p.getProductId().equals(product.getId()))
                        .findFirst()
                        .map(ProductInOrder::getPrice)
                        .orElse(BigDecimal.ZERO));
                return productInfoEmail;
            }).toList();
            orderSentEmailDto.setProducts(productInfoEmails);
            kafkaTemplate.send("order-delivered-request", orderSentEmailDto);
        } catch (Exception e) {
            throw new OrderException("Something went wrong during send email.", e);
        }
    }

    private OrderSentEmailDto findAllOrderData(Order order) {
        try {
            OrderSentEmailDto orderSentEmailDto = new OrderSentEmailDto();
            orderSentEmailDto.setOrderId(String.valueOf(order.getId()));
            orderSentEmailDto.setOrderDate(order.getOrderDate());
            orderSentEmailDto.setCity(order.getOrderAddress().getCity());
            orderSentEmailDto.setAddress(order.getOrderAddress().getAddress());
            orderSentEmailDto.setZipCode(order.getOrderAddress().getZipCode());
            orderSentEmailDto.setCountry(order.getOrderAddress().getCountry());
            orderSentEmailDto.setTotalPrice(order.getTotalPrice());
            CustomerDto customerDto = optCustomer(order.getUserId()).get(5, TimeUnit.SECONDS);
            String email = getUserEmail(order.getUserId()).get(5, TimeUnit.SECONDS);
            orderSentEmailDto.setEmail(email);
            orderSentEmailDto.setFirstName(customerDto.getFirstName());
            orderSentEmailDto.setLastName(customerDto.getLastName());
            return orderSentEmailDto;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new OrderException("Something went wrong during send email.", e);
        }

    }

    private CompletableFuture<String> getUserEmail(String id) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();
        requestsForEmails.put(correlationId, future);
        Long userId = Long.valueOf(id);

        try {
            UserEmailRequest request = new UserEmailRequest(correlationId, userId);
            kafkaTemplate.send("user-email-request", request);
            ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                requestsForEmails.remove(correlationId);
                future.completeExceptionally(new RuntimeException("Timeout waiting for email"));
            }, 5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            requestsForEmails.remove(correlationId);
            future.completeExceptionally(e);
        }
        return future;
    }

    @KafkaListener(topics = "user-email-response", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void userEmailResponse(UserEmailResponse response) {
        CompletableFuture<String> future = requestsForEmails.remove(response.getCorrelationId());
        if (response.getErrorMessage() != null) {
            future.completeExceptionally(new OrderException(response.getErrorMessage()));
            return;
        }
        if (future != null) {
            future.complete(response.getEmail());
        }
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
