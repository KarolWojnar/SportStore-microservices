package com.shop.paymentservice.controller;

import com.shop.paymentservice.model.UrlPaymentResponse;
import com.shop.paymentservice.model.dto.OrderDto;
import com.shop.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    void createPayment_ShouldReturnCreatedStatusAndUrl() {
        String userId = "123";
        OrderDto orderDto = new OrderDto();
        String expectedUrl = "https://stripe.com/checkout";
        when(paymentService.createPayment(any(OrderDto.class), anyString())).thenReturn(expectedUrl);

        ResponseEntity<?> response = paymentController.createPayment(userId, orderDto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertInstanceOf(UrlPaymentResponse.class, response.getBody());
        assertEquals(expectedUrl, ((UrlPaymentResponse) response.getBody()).getUrl());
    }

    @Test
    void createRepayment_ShouldReturnOkStatusAndUrl() {
        String userId = "123";
        String email = "test@example.com";
        String orderId = "order-123";
        String expectedUrl = "https://stripe.com/checkout";
        when(paymentService.createRepayment(anyString(), anyString(), anyString())).thenReturn(expectedUrl);

        ResponseEntity<?> response = paymentController.createRepayment(userId, email, orderId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(UrlPaymentResponse.class, response.getBody());
        assertEquals(expectedUrl, ((UrlPaymentResponse) response.getBody()).getUrl());
    }

    @Test
    void webhook_ShouldProcessPayloadWithoutErrors() {
        String payload = "test-payload";
        String signature = "test-signature";

        paymentController.webhook(payload, signature);
    }
}