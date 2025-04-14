package com.shop.paymentservice.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.shop.paymentservice.exception.PaymentException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
}
