package com.shop.apigateway.filter;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {
    public static final List<String> openApiEndpoints = List.of(
            "/api/auth",
            "/api/auth/login",
            "/api/auth/isLoggedIn",
            "/api/auth/recovery-password",
            "/api/auth/check-reset-code/**",
            "/api/auth/reset-password",
            "/api/auth/activate/**",
            "/images/**",
            "/api/products/**",
            "/api/payment/webhook"

    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> {
                String requestPath = request.getURI().getPath();
                return openApiEndpoints.stream()
                        .noneMatch(uri -> {
                            String regex = uri.replaceAll("\\*\\*", ".*");
                            return requestPath.matches(regex) ||
                                    requestPath.startsWith(uri.replace("/**", ""));
                        });
            };
}
