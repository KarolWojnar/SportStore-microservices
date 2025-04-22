package com.shop.productservice.service;

import com.shop.productservice.exception.ProductException;
import com.shop.productservice.model.dto.*;
import com.shop.productservice.model.entity.Product;
import com.shop.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventService {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    protected final Map<String, CompletableFuture<OrderProductRatedRequest>> orderProductAsRatedMap = new ConcurrentHashMap<>();

    @KafkaListener(topics = "product-cart-quantity-check-request", groupId = "product-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void checkProductAmountAvaibility(ProductQuantityCheck productQuantityCheck) {
        Product product = productRepository
                .findById(productQuantityCheck.getProductId())
                .orElseThrow(() -> new ProductException("Product not found."));
        kafkaTemplate.send(
                "product-cart-quantity-check-response",
                new ProductQuantityCheck(productQuantityCheck.getUserId(),
                        productQuantityCheck.getProductId(),
                        Math.min(product.getAmountLeft(), productQuantityCheck.getQuantity())
                )
        );
    }

    @KafkaListener(topics = "product-cart-info-request", groupId = "product-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleProductInfoRequest(ProductInfoRequest request) {
        try {
            List<Product> products = productRepository.findAllById(request.getProductIds());
            List<ProductBase> productBases = products.stream()
                    .map(ProductBase::mapToBase)
                    .toList();

            ProductInfoResponse response = new ProductInfoResponse(
                    request.getCorrelationId(),
                    productBases
            );

            kafkaTemplate.send("product-cart-info-response", response);
        } catch (Exception e) {
            log.error("Error processing product info request", e);
        }
    }

    @KafkaListener(topics = "cart-validation-request", groupId = "product-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleValidationRequest(CartValidationRequest request) {
        try {
            List<Product> products = productRepository.findAllById(request.getProducts().keySet());

            for (Product product : products) {
                int requestedQuantity = request.getProducts().get(product.getId());
                if (product.getAmountLeft() < requestedQuantity) {
                    throw new ProductException("Not enough products in stock.");
                }
            }

            kafkaTemplate.send("cart-validation-response",
                    new CartValidationResponse(request.getCorrelationId(), null, null));

        } catch (ProductException e) {
            kafkaTemplate.send("cart-validation-response",
                    new CartValidationResponse(request.getCorrelationId(), null, e.getMessage()));
        }
    }

    @Transactional
    @KafkaListener(topics = "total-price-request", groupId = "product-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void getTotalPriceAndBlockProductsRequest(TotalPriceOfProductsRequest request) {
        try {
            List<Product> products = productRepository.findAllById(request.getProducts().keySet());

            BigDecimal totalPrice = BigDecimal.ZERO;
            for (Product product : products) {
                int requestedQuantity = request.getProducts().get(product.getId());
                if (product.getAmountLeft() < requestedQuantity) {
                    throw new ProductException("Not enough products in stock.");
                }
                totalPrice = totalPrice.add(product.getPrice().multiply(BigDecimal.valueOf(requestedQuantity)));
                product.setAmountLeft(product.getAmountLeft() - requestedQuantity);
            }

            productRepository.saveAll(products);

            kafkaTemplate.send("total-price-response",
                    new TotalPriceOfProductsResponse(request.getCorrelationId(), totalPrice, null));

        } catch (ProductException e) {
            kafkaTemplate.send("total-price-response",
                    new TotalPriceOfProductsResponse(request.getCorrelationId(), null, e.getMessage()));
        }
    }

    @Transactional
    @KafkaListener(topics = "total-price-payment-request", groupId = "product-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void getTotalPriceRequest(TotalPriceOfProductsRequest request) {
        try {
            List<Product> products = productRepository.findAllById(request.getProducts().keySet());

            BigDecimal totalPrice = BigDecimal.ZERO;
            for (Product product : products) {
                int requestedQuantity = request.getProducts().get(product.getId());
                if (product.getAmountLeft() < requestedQuantity) {
                    throw new ProductException("Not enough products in stock.");
                }
                totalPrice = totalPrice.add(product.getPrice().multiply(BigDecimal.valueOf(requestedQuantity)));
            }

            kafkaTemplate.send("total-price-payment-response",
                    new TotalPriceOfProductsResponse(request.getCorrelationId(), totalPrice, null));

        } catch (ProductException e) {
            kafkaTemplate.send("total-price-payment-response",
                    new TotalPriceOfProductsResponse(request.getCorrelationId(), null, e.getMessage()));
        }
    }

    @KafkaListener(topics = "products-total-price-by-id-request", groupId = "product-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void getTotalPriceByIdRequest(ProductPriceByIdRequest request) {
        try {
            List<Product> products = productRepository.findAllById(request.getProductIds());
            Map<String, BigDecimal> productPriceMap = new HashMap<>();

            for (Product product : products) {
                productPriceMap.put(product.getId(), product.getPrice());
            }

            kafkaTemplate.send("products-total-price-by-id-response",
                    new ProductPriceByIdResponse(request.getCorrelationId(), null, productPriceMap));

        } catch (Exception e) {
            kafkaTemplate.send("products-total-price-by-id-response",
                    new ProductPriceByIdResponse(request.getCorrelationId(), e.getMessage(), null));
        }
    }

    @Transactional
    @KafkaListener(topics = "order-product-unlock-request", groupId = "product-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderProductUnlockRequest(TotalPriceOfProductsRequest request) {
        try {
            List<Product> products = productRepository.findAllById(request.getProducts().keySet());

            for (Product product : products) {
                int requestedQuantity = request.getProducts().get(product.getId());
                product.setAmountLeft(product.getAmountLeft() + requestedQuantity);
            }

            productRepository.saveAll(products);
            kafkaTemplate.send("order-product-unlock-response",
                    new TotalPriceOfProductsResponse(request.getCorrelationId(), null, null));

        } catch (Exception e) {
            kafkaTemplate.send("order-product-unlock-response",
                    new TotalPriceOfProductsResponse(request.getCorrelationId(), null, e.getMessage()));
            log.error("Error processing order product unlock request", e);
        }
    }

    @KafkaListener(topics = "products-by-id-request", groupId = "product-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void getProductsByIdsRequest(ProductsByIdRequest request) {
        try {
            List<Product> products = productRepository.findAllById(request.getProducts());
            List<ProductOrderDto> productBases = products.stream()
                    .map(ProductOrderDto::mapToDto)
                    .toList();

            ProductsByIdResponse response = new ProductsByIdResponse(
                    request.getCorrelationId(),
                    null,
                    productBases
            );
            kafkaTemplate.send("products-by-id-response", response);
        } catch (Exception e) {
            ProductsByIdResponse response = new ProductsByIdResponse(
                    request.getCorrelationId(),
                    e.getMessage(),
                    null
            );
            kafkaTemplate.send("products-by-id-response", response);
        }
    }

    @KafkaListener(topics = "product-sold-request", groupId = "product-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void incrementSoldItems(Map<String, Integer> request) {
        try {
            List<Product> products = productRepository.findAllById(request.keySet());
            for (Product product : products) {
                product.setOrders(product.getOrders() + request.get(product.getId()));
            }
            productRepository.saveAll(products);
        } catch (Exception e) {
            log.error("Error processing product sold request", e);
        }
    }

    public CompletableFuture<OrderProductRatedRequest> setOrderProductAsRated(String orderId, String productId) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<OrderProductRatedRequest> future = new CompletableFuture<>();
        orderProductAsRatedMap.put(correlationId, future);

        try {
            OrderProductRatedRequest request = new OrderProductRatedRequest(correlationId, orderId, productId);
            kafkaTemplate.send("order-product-rated-request", request);
            ScheduledExecutorService scheduler = java.util.concurrent.Executors.newScheduledThreadPool(1);
            scheduler.schedule(() -> {
                orderProductAsRatedMap.remove(correlationId);
                future.completeExceptionally(new RuntimeException("Timeout waiting for rating"));
            }, 5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            orderProductAsRatedMap.remove(correlationId);
            future.completeExceptionally(e);
        }
        return future;
    }

    @KafkaListener(topics = "order-product-rated-response", groupId = "product-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderProductRatedResponse(String correlationId) {
        CompletableFuture<OrderProductRatedRequest> future = orderProductAsRatedMap.remove(correlationId);
        if (future != null) {
            future.complete(null);
        }
    }
}
