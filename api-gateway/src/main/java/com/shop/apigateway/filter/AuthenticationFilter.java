package com.shop.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator validator;
    private final WebClient webClient;
    private final RedisTemplate<String, String> redisTemplate;

    public AuthenticationFilter(RouteValidator validator, WebClient.Builder webClientBuilder, RedisTemplate<String, String> redisTemplate) {
        super(Config.class);
        this.validator = validator;
        this.webClient = webClientBuilder.build();
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Collections.emptyList();
    }

    @Override
    public GatewayFilter apply(Config config) {
        log.info("AuthenticationFilter applied");
        return ((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            if (!validator.isSecured.test(request)) {
                return chain.filter(exchange);
            }

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.debug("No Authorization header found");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.debug("Invalid Authorization header format");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            String token = authHeader.substring(7);

            String refreshToken = null;
            if (request.getCookies().containsKey("Refresh-token")) {
                refreshToken = request.getCookies().getFirst("Refresh-token").getValue();

                if (refreshToken != null && redisTemplate.hasKey("black_list:" + refreshToken)) {
                    log.debug("Refresh token is blacklisted");
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return response.setComplete();
                }
            }

            if (redisTemplate.hasKey("black_list:" + token)) {
                log.debug("Access token is blacklisted");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            log.info("Response status: {}", response.getStatusCode());
            log.info("Response headers: {}", response.getHeaders());
            String finalRefreshToken = refreshToken;
            return webClient.get()
                    .uri("http://auth-service/api/auth/validate")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .cookies(cookies -> {
                        if (finalRefreshToken != null) {
                            cookies.add("Refresh-token", finalRefreshToken);
                        }
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                        log.error("Validation failed with status: {}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("Token validation failed"));
                    })
                    .bodyToMono(String.class)
                    .flatMap(responseBody -> {
                        log.info("Authentication successful");
                        return chain.filter(exchange);
                    })
                    .onErrorResume(e -> {
                        log.error("Authentication failed: ");
                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                        return response.setComplete();
                    });
        });
    }

    public static class Config {
    }
}