package com.shop.apigateway.model.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ValidationResponseTest {

    @Test
    void testGettersAndSetters() {
        ValidationResponse validationResponse = new ValidationResponse();
        validationResponse.setUserId(1L);
        validationResponse.setEmail("test@example.com");
        validationResponse.setRole("USER");

        assertEquals(1L, validationResponse.getUserId());
        assertEquals("test@example.com", validationResponse.getEmail());
        assertEquals("USER", validationResponse.getRole());
    }

    @Test
    void testConstructor() {
        ValidationResponse validationResponse = new ValidationResponse(1L, "test@example.com", "ROLE_CUSTOMER");

        assertEquals(1L, validationResponse.getUserId());
        assertEquals("test@example.com", validationResponse.getEmail());
        assertEquals("ROLE_CUSTOMER", validationResponse.getRole());
    }

    @Test
    void testNoArgsConstructor() {
        ValidationResponse validationResponse = new ValidationResponse();

        assertNull(validationResponse.getUserId());
        assertNull(validationResponse.getEmail());
        assertNull(validationResponse.getRole());
    }

    @Test
    void testToString() {

        ValidationResponse validationResponse = new ValidationResponse(1L, "test@example.com", "ROLE_CUSTOMER");

        String expectedString = "ValidationResponse{userId=1, email='test@example.com', role='ROLE_CUSTOMER'}";
        assertEquals(expectedString, validationResponse.toString());
    }

}