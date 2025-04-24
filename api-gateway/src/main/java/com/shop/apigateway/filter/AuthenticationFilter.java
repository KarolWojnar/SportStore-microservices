package com.shop.apigateway.filter;

import com.shop.apigateway.model.dto.ValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator validator;
    private final RestTemplate template;

    public AuthenticationFilter(RouteValidator validator, RestTemplate restTemplate) {
        super(Config.class);
        this.validator = validator;
        this.template = restTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!validator.isSecured.test(request)) {
                log.info("Request is not secured, skipping authentication path {}", request.getURI().getPath());
                return chain.filter(exchange);
            }
            log.info("Request is secured, skipping authentication path {}", request.getURI().getPath());

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION) ||
                    request.getHeaders().get(HttpHeaders.AUTHORIZATION).isEmpty() ||
                    !request.getCookies().containsKey("Refresh-token") ||
                    request.getCookies().get("Refresh-token").isEmpty()) {
                return unauthorizedResponse(exchange, "Unauthorized: missing tokens");
            }

            String authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            HttpCookie refreshCookie = request.getCookies().get("Refresh-token").get(0);

            try {
                String cookies = refreshCookie.getName() + "=" + refreshCookie.getValue();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.add(HttpHeaders.COOKIE, cookies);
                headers.add(HttpHeaders.AUTHORIZATION, authHeader);
                HttpEntity<Object> entity = new HttpEntity<>(headers);

                ResponseEntity<ValidationResponse> response = template.exchange(
                        "http://localhost:8080/api/auth/validate",
                        HttpMethod.GET,
                        entity,
                        ValidationResponse.class
                );
                log.info("Validation response: {}", response.getBody());

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    ValidationResponse validData = response.getBody();
                    if (validData != null) {
                        if (validator.requiresAdminRole.test(request) &&
                                !"ROLE_ADMIN".equals(validData.getRole())) {
                            return forbiddenResponse(exchange);
                        }

                        ServerHttpRequest modifiedRequest = request
                                .mutate()
                                .header("X-User-Id", validData.getUserId().toString())
                                .header("X-User-Email", validData.getEmail())
                                .header("X-User-Role", validData.getRole())
                                .build();
                        exchange = exchange.mutate().request(modifiedRequest).build();
                    }
                    return chain.filter(exchange);
                } else {
                    return unauthorizedResponse(exchange, "Unauthorized: invalid tokens");
                }
            } catch (HttpClientErrorException e) {
                log.warn("Authentication failed: {}", e.getMessage());
                return unauthorizedResponse(exchange, "Unauthorized: " + e.getStatusText());
            } catch (Exception e) {
                log.error("Authentication error: {}", e.getMessage(), e);
                return unauthorizedResponse(exchange, "Authorization service unavailable");
            }
        });
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(message.getBytes())));
    }

    private Mono<Void> forbiddenResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap("Forbidden: Admin role required".getBytes())));
    }

    public static class Config {}
}