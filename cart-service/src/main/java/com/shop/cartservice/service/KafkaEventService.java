package com.shop.cartservice.service;

import com.shop.cartservice.model.dto.*;
import com.shop.cartservice.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CartRepository cartRepository;
    private final Map<String, CompletableFuture<List<ProductBase>>> pendingRequests = new ConcurrentHashMap<>();

    public void checkProductQuantity(String userId, String productId, int quantity) {
        try {
            kafkaTemplate.send("product-cart-quantity-check-request",
                    new ProductQuantityCheck(
                            userId,
                            productId,
                            quantity
                    ));
        } catch (Exception e) {
            log.error("Error sending message to Kafka", e);
        }
    }

    @KafkaListener(topics = "product-cart-quantity-check-response", groupId = "cart-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void returnNewValueOfCartQuantity(ProductQuantityCheck productQuantityCheck) {
        cartRepository.setItemQuantity(
                productQuantityCheck.getUserId(),
                productQuantityCheck.getProductId(),
                productQuantityCheck.getQuantity()
        );
    }

    public CompletableFuture<List<ProductBase>> requestProductInfo(List<String> productIds) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<List<ProductBase>> future = new CompletableFuture<>();
        pendingRequests.put(correlationId, future);

        try {
            ProductInfoRequest request = new ProductInfoRequest(correlationId, productIds);
            kafkaTemplate.send("product-cart-info-request", request);

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            executor.schedule(() -> {
                if (!future.isDone()) {
                    future.completeExceptionally(new TimeoutException("Request timed out"));
                    pendingRequests.remove(correlationId);
                }
            }, 5, TimeUnit.SECONDS);

        } catch (Exception e) {
            future.completeExceptionally(e);
            pendingRequests.remove(correlationId);
        }

        return future;
    }

    @KafkaListener(topics = "product-cart-info-response", groupId = "cart-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleProductInfoResponse(ProductInfoResponse response) {
        CompletableFuture<List<ProductBase>> future = pendingRequests.remove(response.getCorrelationId());
        if (future != null) {
            future.complete(response.getProducts());
        }
    }

    @KafkaListener(topics = "cart-items-request", groupId = "cart-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void checkCartHasItems(CartInfoRequest cartInfoRequest) {
        try {
            boolean cartHasItems = cartRepository.existsById(cartInfoRequest.getUserId());
            CartInfoResponse cartInfoResponse = new CartInfoResponse(
                    cartInfoRequest.getCorrelationId(),
                    cartHasItems
            );
            kafkaTemplate.send("cart-items-response", cartInfoResponse);
        } catch (Exception e) {
            log.error("Error processing cart info request", e);
        }
    }
}
