package com.shop.paymentservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.shop.paymentservice.exception.PaymentException;
import com.shop.paymentservice.model.DeliveryTime;
import com.shop.paymentservice.model.dto.*;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private KafkaEventService kafkaEventService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService.stripeSecretKey = "test-secret-key";
        paymentService.stripeWebhookSecret = "test-webhook-secret";
        paymentService.frontUrl = "http://localhost:3000/";
    }

    @Test
    void createPayment_ShouldReturnPaymentUrl() {
        OrderDto orderDto = OrderDto.builder()
                .email("test@example.com")
                .deliveryTime(DeliveryTime.STANDARD)
                .paymentMethod(SessionCreateParams.PaymentMethodType.CARD)
                .build();
        String userId = "123";
        Map<String, Integer> products = Map.of("product-1", 2);
        BigDecimal totalPrice = new BigDecimal("100.00");
        String orderId = "order-123";
        String expectedUrl = "https://stripe.com/checkout";

        when(kafkaEventService.getCartProducts(userId))
                .thenReturn(CompletableFuture.completedFuture(products));
        when(kafkaEventService.getTotalPriceOfCart(products))
                .thenReturn(CompletableFuture.completedFuture(totalPrice));
        when(kafkaEventService.createOrder(any()))
                .thenReturn(CompletableFuture.completedFuture(orderId));
        when(kafkaEventService.createOrUpdateCustomerInfo(any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(kafkaEventService.deleteCart(userId))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(kafkaEventService.setSessionIdForOrder(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        Session mockSession = mock(Session.class);
        when(mockSession.getUrl()).thenReturn(expectedUrl);
        when(mockSession.getId()).thenReturn("session-id");

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            String result = paymentService.createPayment(orderDto, userId);

            assertEquals(expectedUrl, result);
        }
    }

    @Test
    void createPayment_ShouldThrowPaymentExceptionWhenStripeFails() {
        OrderDto orderDto = OrderDto.builder()
                .email("test@example.com")
                .deliveryTime(DeliveryTime.STANDARD)
                .paymentMethod(SessionCreateParams.PaymentMethodType.CARD)
                .build();
        String userId = "123";
        Map<String, Integer> products = Map.of("product-1", 2);
        BigDecimal totalPrice = new BigDecimal("100.00");
        String orderId = "order-123";

        when(kafkaEventService.getCartProducts(userId))
                .thenReturn(CompletableFuture.completedFuture(products));
        when(kafkaEventService.getTotalPriceOfCart(products))
                .thenReturn(CompletableFuture.completedFuture(totalPrice));
        when(kafkaEventService.createOrder(any()))
                .thenReturn(CompletableFuture.completedFuture(orderId));
        when(kafkaEventService.createOrUpdateCustomerInfo(any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenThrow(new RuntimeException("Stripe error"));

            assertThrows(PaymentException.class, () -> paymentService.createPayment(orderDto, userId));
        }
    }

    @Test
    void createRepayment_ShouldReturnPaymentUrl() throws Exception {
        String orderId = "order-123";
        String userId = "user-123";
        String email = "test@example.com";
        OrderInfoRepayment orderInfo = new OrderInfoRepayment(
                SessionCreateParams.PaymentMethodType.CARD,
                new BigDecimal("100.00")
        );
        String expectedUrl = "https://stripe.com/checkout";

        when(kafkaEventService.getOrderInfo(orderId, userId))
                .thenReturn(CompletableFuture.completedFuture(orderInfo));
        when(kafkaEventService.setSessionIdForOrder(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        Session mockSession = mock(Session.class);
        when(mockSession.getUrl()).thenReturn(expectedUrl);
        when(mockSession.getId()).thenReturn("session-123");

        try (MockedStatic<Session> mockedSession = mockStatic(Session.class)) {
            mockedSession.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            String result = paymentService.createRepayment(orderId, userId, email);

            assertEquals(expectedUrl, result);
            verify(kafkaEventService).getOrderInfo(orderId, userId);
        }
    }

    @Test
    void webhook_ShouldProcessEventSuccessfully() throws JsonProcessingException {
        String payload = "{\"id\":\"evt_123\",\"type\":\"checkout.session.completed\",\"data\":{\"object\":{\"id\":\"session-123\"}}}";
        String signature = "valid-signature";

        JsonNode mockedJsonNode = mock(JsonNode.class);
        when(objectMapper.readTree(anyString())).thenReturn(mockedJsonNode);
        when(mockedJsonNode.get("id")).thenReturn(new TextNode("session-123"));

        try (MockedStatic<Webhook> mockedWebhook = mockStatic(Webhook.class)) {
            Event event = mock(Event.class);
            when(event.getType()).thenReturn("checkout.session.completed");

            EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getRawJson()).thenReturn("{\"id\":\"session-123\"}");
            when(objectMapper.registerModule(any(ParameterNamesModule.class))).thenReturn(objectMapper);
            when(objectMapper.registerModule(any(JavaTimeModule.class))).thenReturn(objectMapper);
            when(objectMapper.setVisibility(any(), any())).thenReturn(objectMapper);
            when(objectMapper.readTree(anyString())).thenReturn(mockedJsonNode);

            mockedWebhook.when(() -> Webhook.constructEvent(eq(payload), eq(signature), anyString()))
                    .thenReturn(event);

            assertDoesNotThrow(() -> paymentService.webhook(payload, signature));

            verify(kafkaEventService).sendOrderAsProcessing("session-123");
        }
    }

    @Test
    void webhook_ShouldThrowPaymentExceptionWhenProcessingFails() {
        String payload = "invalid-payload";
        String signature = "invalid-signature";

        assertThrows(PaymentException.class, () -> paymentService.webhook(payload, signature));
    }
}