package com.shop.paymentservice.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.shop.paymentservice.exception.PaymentException;
import com.shop.paymentservice.model.DeliveryTime;
import com.shop.paymentservice.model.dto.CustomerFromOrderDto;
import com.shop.paymentservice.model.dto.OrderBaseInfo;
import com.shop.paymentservice.model.dto.OrderDto;
import com.shop.paymentservice.model.dto.OrderInfoRepayment;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final KafkaEventService kafkaEventService;

    private final ObjectMapper objectMapper;
    @Value("${spring.stripe.secret}")
    private String stripeSecretKey;
    @Value("${spring.webhook.secret}")
    private String stripeWebhookSecret;
    @Value("${front.url}")
    private String frontUrl;

    public void webhook(String payload, String signature) {
        try {
            Event event = Webhook.constructEvent(payload, signature, stripeWebhookSecret);
            if (event.getType().equals("checkout.session.completed")) {

                String rawJson = event.getDataObjectDeserializer().getRawJson();

                String sessionId = objectMapper.registerModule(new ParameterNamesModule())
                        .registerModule(new JavaTimeModule())
                        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                        .readTree(rawJson).get("id").asText();

                kafkaEventService.sendOrderAsProcessing(sessionId);
            }
        } catch (Exception e) {
            throw new PaymentException("Error during payment." + e.getMessage());
        }
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
            kafkaEventService.setSessionIdForOrder(orderId, session.getId())
                    .get(10, TimeUnit.SECONDS);
            return session.getUrl();
        } catch (StripeException e) {
            throw new PaymentException("Error during payment.", e);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            throw new PaymentException("Error with order payment.", e);
        }
    }

    public String createPayment(OrderDto orderDto, String userId) {
        try {
            log.info("Start payment for {}", userId);
            Map<String, Integer> products = kafkaEventService.getCartProducts(userId)
                    .get(5, TimeUnit.SECONDS);
            BigDecimal shippingPrice = orderDto.getDeliveryTime().equals(DeliveryTime.STANDARD)
                    ? BigDecimal.ZERO
                    : new BigDecimal("10.00");
            log.info("products get: {}", products.size());
            BigDecimal cartTotal = kafkaEventService.getTotalPriceOfCart(products)
                    .get(10, TimeUnit.SECONDS);
            BigDecimal totalPrice = cartTotal.add(shippingPrice)
                    .multiply(new BigDecimal("100"))
                    .setScale(0, RoundingMode.HALF_UP);
            log.info("total price: {}", totalPrice);
            OrderBaseInfo order = new OrderBaseInfo(
                    products,
                    userId,
                    orderDto.getShippingAddress(),
                    totalPrice,
                    orderDto.getPaymentMethod()
            );
            CustomerFromOrderDto customer = new CustomerFromOrderDto(
                    Long.valueOf(userId),
                    orderDto.getFirstName(),
                    orderDto.getLastName(),
                    orderDto.getShippingAddress()
            );
            kafkaEventService.createOrUpdateCustomerInfo(customer).get(5, TimeUnit.SECONDS);
            String orderId = kafkaEventService.createOrder(order).get(5, TimeUnit.SECONDS);
            log.info("order created: {}", orderId);
            String url = preparePaymentTemplate(orderDto, totalPrice.longValueExact(), orderId);
            kafkaEventService.deleteCart(userId).get(5, TimeUnit.SECONDS);
            return url;
        } catch (Exception e) {
            throw new PaymentException("Error during payment.", e);
        }
    }

    public String createRepayment(String orderId, String userId, String email) {
        try {
            OrderInfoRepayment orderInfoDto = kafkaEventService.getOrderInfo(orderId, userId)
                    .get(5, TimeUnit.SECONDS);
            OrderDto orderDto = OrderDto.builder()
                    .email(email)
                    .paymentMethod(orderInfoDto.getPaymentMethod())
                    .totalPrice(orderInfoDto.getTotalPrice())
                    .build();
            BigDecimal totalPriceInCents = orderDto.getTotalPrice()
                    .multiply(new BigDecimal("100"))
                    .setScale(0, RoundingMode.HALF_UP);
            return preparePaymentTemplate(orderDto, totalPriceInCents.longValueExact(), orderId);
        } catch (Exception ex) {
            throw new PaymentException("Error during create repayment", ex);
        }
    }
}
