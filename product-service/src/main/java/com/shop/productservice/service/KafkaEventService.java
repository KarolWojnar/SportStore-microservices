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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventService {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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
}
