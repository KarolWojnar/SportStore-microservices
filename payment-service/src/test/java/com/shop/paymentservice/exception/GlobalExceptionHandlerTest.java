package com.shop.paymentservice.exception;

import com.shop.paymentservice.model.ErrorResponse;
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

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    void handleUsernameNotFoundException_ShouldReturnNotFoundResponse() {
        String errorMessage = "User not found";
        UsernameNotFoundException exception = new UsernameNotFoundException(errorMessage);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUsernameNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getDetails());
    }

    @Test
    void handlePaymentException_ShouldReturnBadRequestResponse() {
        String errorMessage = "Payment error";
        PaymentException exception = new PaymentException(errorMessage);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handlePaymentException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(errorMessage, response.getBody().getMessage());
        assertNull(response.getBody().getDetails());
    }

    @Test
    void handleRuntimeException_ShouldReturnInternalServerErrorResponse() {
        RuntimeException exception = new RuntimeException("Runtime error");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntimeException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
        assertNull(response.getBody().getDetails());
    }

    @Test
    void handleValidationExceptions_ShouldReturnBadRequestWithDetails() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "default message");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationExceptions(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().getMessage());
        Map<String, String> details = response.getBody().getDetails();
        assertNotNull(details);
        assertEquals(1, details.size());
        assertEquals("default message", details.get("field"));
    }
}