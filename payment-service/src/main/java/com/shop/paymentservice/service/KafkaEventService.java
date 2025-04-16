package com.shop.paymentservice.service;

import com.shop.paymentservice.exception.PaymentException;
import com.shop.paymentservice.model.dto.*;
import com.shop.paymentservice.model.model.OutboxEvent;
import com.shop.paymentservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaEventService {

    private final Map<String, CompletableFuture<BigDecimal>> requestsForTotalPrice = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<OrderInfoRepayment>> requestsForOrderInfo = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Map<String, Integer>>> requestsForCart = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> requestsForCreateOrder = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Void>> requestsForDeleteCart = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Void>> requestsForOrderSession = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxRepository outboxRepository;

    public void sendOrderAsProcessing(String sessionId) {
        log.info("Order with id {} is processing", sessionId);
        OutboxEvent event = new OutboxEvent("order-paid", sessionId);
        outboxRepository.save(event);
    }

    public CompletableFuture<BigDecimal> getTotalPriceOfCart(Map<String, Integer> products) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<BigDecimal> future = new CompletableFuture<>();
        requestsForTotalPrice.put(correlationId, future);
        try {
            TotalPriceOfProductsRequest request = new TotalPriceOfProductsRequest(correlationId, products);
            kafkaTemplate.send("total-price-payment-request", request);
            ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                requestsForTotalPrice.remove(correlationId);
                future.completeExceptionally(new RuntimeException("Timeout waiting for total price"));
            }, 10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            requestsForTotalPrice.remove(correlationId);
            future.completeExceptionally(e);
        }
        return future;
    }

    @KafkaListener(topics = "total-price-payment-response", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void totalPriceResponse(TotalPriceOfProductsResponse response) {
        CompletableFuture<BigDecimal> future = requestsForTotalPrice.remove(response.getCorrelationId());
        if (response.getErrorMessage() != null) {
            future.completeExceptionally(new PaymentException(response.getErrorMessage()));
            return;
        }
        if (future != null) {
            future.complete(response.getTotalPrice());
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void clearEvents() {
        log.info("Clearing events");
        LocalDateTime time = LocalDateTime.now().minusDays(2);
        outboxRepository.deleteAllBySentTrueAndSentAtBefore(time);
        outboxRepository.deleteAll();
    }

    @Transactional
    @Scheduled(cron = "*/5 * * * * *")
    public void trySendEvents() {
        List<OutboxEvent> events = outboxRepository.findAllBySentFalse();
        events.forEach(event -> {
            try {
                kafkaTemplate.send(event.getTopic(), event.getPayload());
                event.setSent(true);
                event.setSentAt(LocalDateTime.now());
                outboxRepository.save(event);
            } catch (Exception e) {
                log.error("Error sending event: {}", event.getTopic(), e);
            }
        });
        if (!events.isEmpty()) {
            log.info("Sent {} events", events.size());
        }
    }

    public CompletableFuture<Map<String, Integer>> getCartProducts(String userId) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<Map<String, Integer>> future = new CompletableFuture<>();
        requestsForCart.put(correlationId, future);
        try {
            kafkaTemplate.send(
                    "cart-product-payment-request",
                    new ProductsInCartInfoRequest(correlationId, userId));

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

    @KafkaListener(topics = "cart-product-payment-response", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void cartResponse(ProductsInCartInfoResponse response) {
        CompletableFuture<Map<String, Integer>> future = requestsForCart.remove(response.getCorrelationId());
        if (future != null) {
            future.complete(response.getProduct());
        }
    }

    public CompletableFuture<String> createOrder(OrderBaseInfo order) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();
        requestsForCreateOrder.put(correlationId, future);
        try {
            kafkaTemplate.send("order-create-request", new CreateOrderRequest(correlationId, order));
            ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                future.completeExceptionally(new RuntimeException("Timeout waiting for order"));
            }, 5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @KafkaListener(topics = "order-create-response", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void orderResponse(CreateOrderResponse response) {
        CompletableFuture<String> future = requestsForCreateOrder.remove(response.getCorrelationId());
        if (future != null) {
            future.complete(response.getOrderId());
        }
    }

    public CompletableFuture<Void> deleteCart(String userId) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<Void> future = new CompletableFuture<>();
        requestsForDeleteCart.put(correlationId, future);
        try {
            kafkaTemplate.send("cart-delete-request", new ProductsInCartInfoRequest(correlationId, userId));
            ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                future.completeExceptionally(new RuntimeException("Timeout waiting for order"));
            }, 5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @KafkaListener(topics = "cart-delete-response", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void cartDeleteResponse(String correlationId) {
        CompletableFuture<Void> future = requestsForDeleteCart.remove(correlationId);
        if (future != null) {
            future.complete(null);
        }
    }

    public CompletableFuture<Void> setSessionIdForOrder(String orderId, String sessionId) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<Void> future = new CompletableFuture<>();
        requestsForOrderSession.put(correlationId, future);
        log.info("123");
        try {
            OrderSessionRequest request = new OrderSessionRequest(correlationId, sessionId, orderId);
            kafkaTemplate.send("order-session-request", request);
            ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                future.completeExceptionally(new RuntimeException("Timeout waiting for order"));
            }, 10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @KafkaListener(topics = "order-session-response", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void orderSessionResponse(String correlationId) {
        log.info("order saved");
        CompletableFuture<Void> future = requestsForOrderSession.remove(correlationId);
        if (future != null) {
            future.complete(null);
        }
    }

    public CompletableFuture<OrderInfoRepayment> getOrderInfo(String orderId, String userId) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<OrderInfoRepayment> future = new CompletableFuture<>();
        requestsForOrderInfo.put(correlationId, future);
        try {
            OrderRepaymentRequest request = new OrderRepaymentRequest(orderId, userId, correlationId);
            kafkaTemplate.send("order-info-request", request);
            ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                future.completeExceptionally(new RuntimeException("Timeout waiting for order"));
            }, 10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @KafkaListener(topics = "order-info-response", groupId = "order-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void orderInfoResponse(OrderRepaymentResponse order) {
        CompletableFuture<OrderInfoRepayment> future = requestsForOrderInfo.remove(order.getCorrelationId());
        if (order.getErrorMessage() != null) {
            future.completeExceptionally(new PaymentException(order.getErrorMessage()));
            return;
        }
        log.info(String.valueOf(order.getOrderInfoRepayment().getTotalPrice()));
        log.info(String.valueOf(order.getOrderInfoRepayment().getPaymentMethod()));
        if (future != null) {
            future.complete(order.getOrderInfoRepayment());
        }
    }
}
