package com.shop.apigateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {
    public static final List<String> openApiEndpoints = List.of(
            "/api/auth",
            "/api/auth/validate",
            "/api/auth/login",
            "/api/auth/google",
            "/api/auth/isLoggedIn",
            "/api/auth/recovery-password",
            "/api/auth/check-reset-code/**",
            "/api/auth/reset-password",
            "/api/auth/activate/**",
            "/images/**",
            "/api/products/**",
            "/api/payment/webhook"
    );

    public static final List<String> adminEndpoints = List.of(
            "/api/orders/admin",
            "/api/orders/admin/**",
            "/api/products/admin",
            "/api/products/admin/**",
            "/api/users/admin",
            "/api/users/admin/**",
            "/api/auth/admin/**"
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> {
                String path = request.getURI().getPath();
                if (adminEndpoints.stream().anyMatch(endpoint -> pathMatches(endpoint, path))) {
                    return true;
                }
                return openApiEndpoints.stream()
                        .noneMatch(endpoint -> pathMatches(endpoint, path));
            };

    public Predicate<ServerHttpRequest> requiresAdminRole =
            request -> adminEndpoints.stream()
                    .anyMatch(endpoint -> pathMatches(endpoint, request.getURI().getPath()));

    private boolean pathMatches(String pattern, String path) {
        if (pattern.equals(path)) return true;

        String regex = pattern
                .replaceAll("\\*\\*", ".*")
                .replaceAll("/\\*", "/[^/]*");

        return path.matches(regex);
    }
}
