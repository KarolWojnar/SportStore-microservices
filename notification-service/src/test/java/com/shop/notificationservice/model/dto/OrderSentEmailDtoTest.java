package com.shop.notificationservice.model.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderSentEmailDtoTest {

    @Test
    void orderSentEmailDto_shouldHaveCorrectFields() {
        ProductInfoEmail product = new ProductInfoEmail(
                "prod-123", "Test Product", 2, new BigDecimal("19.99"), false
        );

        Date orderDate = new Date();

        OrderSentEmailDto dto = new OrderSentEmailDto(
                "John",
                "Doe",
                "test@example.com",
                "order-123",
                orderDate,
                "123 Main St",
                "12345",
                "New York",
                "USA",
                new BigDecimal("39.98"),
                List.of(product)
        );

        assertEquals("John", dto.getFirstName());
        assertEquals("Doe", dto.getLastName());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("order-123", dto.getOrderId());
        assertEquals(orderDate, dto.getOrderDate());
        assertEquals("123 Main St", dto.getAddress());
        assertEquals("12345", dto.getZipCode());
        assertEquals("New York", dto.getCity());
        assertEquals("USA", dto.getCountry());
        assertEquals(new BigDecimal("39.98"), dto.getTotalPrice());
        assertEquals(1, dto.getProducts().size());
    }
}