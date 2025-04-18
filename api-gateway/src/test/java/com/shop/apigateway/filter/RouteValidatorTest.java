package com.shop.apigateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

import static org.junit.jupiter.api.Assertions.*;

class RouteValidatorTest {

    private final RouteValidator routeValidator = new RouteValidator();

    @Test
    public void testIsSecured_OpenEndpoint() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/auth/login").build();
        assertFalse(routeValidator.isSecured.test(request));
    }

    @Test
    public void testIsSecured_SecuredEndpoint() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders").build();
        assertTrue(routeValidator.isSecured.test(request));
    }

    @Test
    public void testRequiresAdminRole_AdminEndpoint() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders/admin").build();
        assertTrue(routeValidator.requiresAdminRole.test(request));
    }

    @Test
    public void testRequiresAdminRole_NonAdminEndpoint() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/orders").build();
        assertFalse(routeValidator.requiresAdminRole.test(request));
    }

    @Test
    public void testPathMatches() {
        assertTrue(routeValidator.pathMatches("/api/products/**", "/api/products/123"));
        assertFalse(routeValidator.pathMatches("/api/products/**", "/api/orders"));
    }
}