package com.shop.orderservice.exception;

import com.shop.orderservice.model.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    void handleUsernameNotFoundException_shouldReturnNotFoundResponse() {
        String errorMessage = "User not found";
        UsernameNotFoundException ex = new UsernameNotFoundException(errorMessage);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUsernameNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getDetails());
    }

    @Test
    void handleIllegalArgumentException_shouldReturnBadRequestResponse() {
        String errorMessage = "Invalid argument";
        IllegalArgumentException ex = new IllegalArgumentException(errorMessage);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getDetails());
    }

    @Test
    void handleRuntimeException_shouldReturnInternalServerErrorResponse() {
        String errorMessage = "Runtime error";
        RuntimeException ex = new RuntimeException(errorMessage);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntimeException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
        assertNull(response.getBody().getDetails());
    }

    @Test
    void handleGenericException_shouldReturnInternalServerErrorWithMessage() {
        String errorMessage = "Generic error";
        Exception ex = new Exception(errorMessage);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred: " + errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getDetails());
    }

    @Test
    void handleOrderException_shouldReturnBadRequestResponse() {
        String errorMessage = "Order error";
        OrderException ex = new OrderException(errorMessage);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCustomerException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getDetails());
    }

    @Test
    void handleOrderExceptionWithCause_shouldReturnBadRequestResponse() {
        String errorMessage = "Order error with cause";
        Throwable cause = new RuntimeException("Root cause");
        OrderException ex = new OrderException(errorMessage, cause);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleCustomerException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getDetails());
    }

    @Test
    void handleMethodArgumentNotValidException_shouldReturnBadRequestWithDetails() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError1 = new FieldError("object", "field1", "must not be null");
        FieldError fieldError2 = new FieldError("object", "field2", "must be positive");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Arrays.asList(fieldError1, fieldError2));

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().getMessage());

        Map<String, String> details = response.getBody().getDetails();
        assertNotNull(details);
        assertEquals(2, details.size());
        assertEquals("must not be null", details.get("field1"));
        assertEquals("must be positive", details.get("field2"));
    }
}