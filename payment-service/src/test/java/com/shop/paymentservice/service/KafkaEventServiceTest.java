package com.shop.paymentservice.service;

import com.shop.paymentservice.model.ShippingAddress;
import com.shop.paymentservice.model.dto.*;
import com.shop.paymentservice.model.model.OutboxEvent;
import com.shop.paymentservice.repository.OutboxRepository;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private KafkaEventService kafkaEventService;

    @Test
    void sendOrderAsProcessing_ShouldSaveOutboxEvent() {
        String sessionId = "session-123";

        kafkaEventService.sendOrderAsProcessing(sessionId);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEvent savedEvent = captor.getValue();
        assertEquals("order-paid", savedEvent.getTopic());
        assertEquals(sessionId, savedEvent.getPayload());
        assertFalse(savedEvent.isSent());
    }

    @Test
    void getTotalPriceOfCart_ShouldSendRequestAndReturnFuture() {
        Map<String, Integer> products = Map.of("product-1", 2);
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        CompletableFuture<BigDecimal> future = kafkaEventService.getTotalPriceOfCart(products);

        assertNotNull(future);
        verify(kafkaTemplate).send(eq("total-price-payment-request"), any(TotalPriceOfProductsRequest.class));
    }

    @Test
    void totalPriceResponse_ShouldCompleteFuture() {
        String correlationId = UUID.randomUUID().toString();
        BigDecimal expectedPrice = new BigDecimal("100.00");
        TotalPriceOfProductsResponse response = new TotalPriceOfProductsResponse(correlationId, expectedPrice, null);

        CompletableFuture<BigDecimal> future = new CompletableFuture<>();
        kafkaEventService.requestsForTotalPrice.put(correlationId, future);

        kafkaEventService.totalPriceResponse(response);

        assertTrue(future.isDone());
        assertEquals(expectedPrice, future.join());
    }

    @Test
    void getCartProducts_ShouldSendRequestAndReturnFuture() {
        String userId = "user-123";
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        CompletableFuture<Map<String, Integer>> future = kafkaEventService.getCartProducts(userId);

        assertNotNull(future);
        verify(kafkaTemplate).send(eq("cart-product-payment-request"), any(ProductsInCartInfoRequest.class));
    }

    @Test
    void cartResponse_ShouldCompleteFuture() {
        String correlationId = UUID.randomUUID().toString();
        Map<String, Integer> expectedProducts = Map.of("product-1", 2);
        ProductsInCartInfoResponse response = new ProductsInCartInfoResponse(correlationId, expectedProducts);

        CompletableFuture<Map<String, Integer>> future = new CompletableFuture<>();
        kafkaEventService.requestsForCart.put(correlationId, future);

        kafkaEventService.cartResponse(response);

        assertTrue(future.isDone());
        assertEquals(expectedProducts, future.join());
    }

    @Test
    void createOrder_ShouldSendRequestAndReturnFuture() {
        ShippingAddress shippingAddress = new ShippingAddress("address", "city", "country", "01100");
        OrderBaseInfo order = new OrderBaseInfo(
                Map.of("product-1", 2),
                "user-123",
                shippingAddress,
                new BigDecimal("100.00"),
                SessionCreateParams.PaymentMethodType.CARD
        );
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        CompletableFuture<String> future = kafkaEventService.createOrder(order);

        assertNotNull(future);
        verify(kafkaTemplate).send(eq("order-create-request"), any(CreateOrderRequest.class));
    }

    @Test
    void orderResponse_ShouldCompleteFuture() {
        String correlationId = UUID.randomUUID().toString();
        String orderId = "order-123";
        CreateOrderResponse response = new CreateOrderResponse(correlationId, orderId);

        CompletableFuture<String> future = new CompletableFuture<>();
        kafkaEventService.requestsForCreateOrder.put(correlationId, future);

        kafkaEventService.orderResponse(response);

        assertTrue(future.isDone());
        assertEquals(orderId, future.join());
    }

    @Test
    void setSessionIdForOrder_ShouldSendRequestAndReturnFuture() {
        String orderId = "order-123";
        String sessionId = "session-123";
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        CompletableFuture<Void> future = kafkaEventService.setSessionIdForOrder(orderId, sessionId);

        assertNotNull(future);
        verify(kafkaTemplate).send(eq("order-session-request"), any(OrderSessionRequest.class));
    }

    @Test
    void orderSessionResponse_ShouldCompleteFuture() {
        String correlationId = UUID.randomUUID().toString();

        CompletableFuture<Void> future = new CompletableFuture<>();
        kafkaEventService.requestsForOrderSession.put(correlationId, future);

        kafkaEventService.orderSessionResponse(correlationId);

        assertTrue(future.isDone());
        assertNull(future.join());
    }
}