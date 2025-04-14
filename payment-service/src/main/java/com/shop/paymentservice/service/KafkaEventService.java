package com.shop.paymentservice.service;

import com.shop.paymentservice.exception.PaymentException;
import com.shop.paymentservice.model.DeliveryTime;
import com.shop.paymentservice.model.model.OutboxEvent;
import com.shop.paymentservice.model.dto.OrderDto;
import com.shop.paymentservice.repository.OutboxRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaEventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutboxRepository outboxRepository;
    @Value("${spring.stripe.secret}")
    private String stripeSecretKey;
    @Value("${front.url}")
    private String frontUrl;

    public void sendOrderAsProcessing(String sessionId) {
        log.info("Order with id {} is processing", sessionId);
        OutboxEvent event = new OutboxEvent("order-paid", sessionId);
        outboxRepository.save(event);
    }

    @KafkaListener(topics = "order-create-payment", groupId = "payment-service")
    public String createPayment(OrderDto orderDto) {
        BigDecimal shippingPrice = orderDto.getDeliveryTime().equals(DeliveryTime.STANDARD)
                ? BigDecimal.ZERO
                : new BigDecimal("10.00");

        BigDecimal cartTotal = orderDto.getTotalPrice();
        BigDecimal totalPrice = cartTotal.add(shippingPrice)
                .multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP);

        return preparePaymentTemplate(orderDto, totalPrice.longValueExact(), orderDto.getId());
    }

    @KafkaListener(topics = "order-create-payment", groupId = "payment-service")
    public String createRepayment(OrderDto orderDto) {
        BigDecimal totalPriceInCents = orderDto.getTotalPrice()
                .multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP);
        return preparePaymentTemplate(orderDto, totalPriceInCents.longValueExact(), orderDto.getId());
    }

    private String preparePaymentTemplate(OrderDto orderDto, long totalPrice, String orderId) {
        Stripe.apiKey = stripeSecretKey;
        try {
            var productData = com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData.ProductData.builder()
                    .setName("SportWebStore")
                    .build();

            var priceData = com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency("eur")
                    .setUnitAmount(totalPrice)
                    .setProductData(productData)
                    .build();

            var items = com.stripe.param.checkout.SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(priceData)
                    .build();

            com.stripe.param.checkout.SessionCreateParams sessionCreateParams =
                    com.stripe.param.checkout.SessionCreateParams.builder()
                            .setMode(com.stripe.param.checkout.SessionCreateParams.Mode.PAYMENT)
                            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.valueOf(orderDto.getPaymentMethod().name()))
                            .setCustomerEmail(orderDto.getEmail())
                            .setSuccessUrl(frontUrl + "order?paid=true&orderId=" + orderId)
                            .setCancelUrl(frontUrl + "order?paid=false&orderId=" + orderId)
                            .addLineItem(items)
                            .build();

            Session session = com.stripe.model.checkout.Session.create(sessionCreateParams);
            return session.getUrl();
        } catch (StripeException e) {
            throw new PaymentException("Error during payment.");
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

}
