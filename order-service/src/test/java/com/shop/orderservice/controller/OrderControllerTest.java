package com.shop.orderservice.controller;

import com.shop.orderservice.model.dto.OrderBaseInfoDto;
import com.shop.orderservice.model.dto.OrderDto;
import com.shop.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @InjectMocks
    private OrderController orderController;

    @Mock
    private OrderService orderService;

    @Test
    void getSummary_Success() {
        String userId = "123";
        OrderDto mockResponse = new OrderDto();
        when(orderService.getSummary(userId)).thenReturn(mockResponse);

        ResponseEntity<?> response = orderController.getSummary(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(orderService, times(1)).getSummary(userId);
    }

    @Test
    void cancelPayment_Success() {
        String userId = "123";

        ResponseEntity<?> response = orderController.cancelPayment(userId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(orderService, times(1)).cancelPayment(userId);
    }

    @Test
    void getAllOrdersByUser_Success() {
        String userId = "123";
        List<OrderBaseInfoDto> mockOrders = List.of(new OrderBaseInfoDto());
        when(orderService.getUserOrders(userId)).thenReturn(mockOrders);

        ResponseEntity<?> response = orderController.getAllOrdersByUser(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockOrders, response.getBody());
        verify(orderService, times(1)).getUserOrders(userId);
    }
}