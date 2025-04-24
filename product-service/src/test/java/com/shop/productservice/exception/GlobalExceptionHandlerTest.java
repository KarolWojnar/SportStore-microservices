package com.shop.productservice.exception;

import com.shop.productservice.model.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Test
    void handleUsernameNotFoundException_ShouldReturnNotFound() {
        UsernameNotFoundException exception = new UsernameNotFoundException("User not found");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUsernameNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found", response.getBody().getMessage());
    }

    @Test
    void handleMaxUploadSizeExceededException_ShouldReturnPayloadTooLarge() {
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMaxUploadSizeExceededException();

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertEquals("File size exceeds the maximum allowed size", response.getBody().getMessage());
    }

    @Test
    void handleProductException_ShouldReturnBadRequest() {
        ProductException exception = new ProductException("Product error");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleProductException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Product error", response.getBody().getMessage());
    }

    @Test
    void handleAuthorizationDeniedException_ShouldReturnUnauthorized() {
        AuthorizationDeniedException exception = new AuthorizationDeniedException("Unauthorized");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleOrderException(exception);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Unauthorized", response.getBody().getMessage());
    }

    @Test
    void handleRuntimeException_ShouldReturnInternalServerError() {
        RuntimeException exception = new RuntimeException("Runtime error");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleRuntimeException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
    }
}