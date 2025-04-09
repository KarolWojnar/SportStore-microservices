package com.shop.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final RouteValidator validator;
    private final RestTemplate template;
    private final RedisTemplate<String, String> redisTemplate;

    public AuthenticationFilter(RouteValidator validator, RestTemplate restTemplate, RedisTemplate<String, String> redisTemplate) {
        super(Config.class);
        this.validator = validator;
        this.template = restTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            log.info("--START GatewayFilter");

            if (validator.isSecured.test(exchange.getRequest())) {
                if (!exchange.getRequest().getCookies().containsKey(HttpHeaders.AUTHORIZATION)
                        && !exchange.getRequest().getCookies().containsKey("Refresh-token")) {
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                            .bufferFactory()
                            .wrap(("Unauthorized: error with tokens").getBytes())));
                }

                String authCookie = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                HttpCookie refreshCookie = exchange.getRequest().getCookies().get("Refresh-token").get(0);

                if (refreshCookie.getValue() != null && redisTemplate.hasKey("jwt:blacklist:refresh:" + refreshCookie.getValue())) {
                        log.debug("Refresh token is blacklisted");
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().writeWith(Mono.just(
                                exchange.getResponse().bufferFactory().wrap("Unauthorized: Blacklisted refresh token".getBytes())));
                    }

                if (authCookie != null && redisTemplate.hasKey("jwt:blacklist:access:" + authCookie)) {
                    log.debug("Access token is blacklisted");
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().writeWith(Mono.just(
                            exchange.getResponse().bufferFactory().wrap("Unauthorized: Blacklisted access token".getBytes())));
                }

                try {
                    String cookies = refreshCookie.getName() +
                            "=" +
                            refreshCookie.getValue();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.add(HttpHeaders.COOKIE, cookies);
                    assert authCookie != null;
                    headers.add(HttpHeaders.AUTHORIZATION, authCookie);
                    HttpEntity<Object> entity = new HttpEntity<>(headers);
                    ResponseEntity<String> response = template.exchange(
                            "http://localhost:8080/api/auth/validate",
                            HttpMethod.GET,
                            entity,
                            String.class
                    );
                    if (response.getStatusCode() == HttpStatus.OK) {
                        log.info("Authentication successful, proceeding with chain filter");
                        List<String> cookiesList = response.getHeaders().get(HttpHeaders.SET_COOKIE);
                        if (cookiesList != null) {
                            List<java.net.HttpCookie> httpCookie = java.net.HttpCookie.parse(cookiesList.get(0));
                            for (java.net.HttpCookie cookie: httpCookie){
                                exchange.getResponse().getCookies().add(cookie.getName(),
                                        ResponseCookie.from(cookie.getName(),cookie.getValue())
                                                .domain(cookie.getDomain())
                                                .path(cookie.getPath())
                                                .maxAge(cookie.getMaxAge())
                                                .secure(cookie.getSecure())
                                                .httpOnly(cookie.isHttpOnly())
                                                .build());
                            }
                        }
                        log.info("Successful login");
                        return chain.filter(exchange);
                    }

                } catch (HttpClientErrorException e) {
                    log.warn("Can't login bad token");
                    String message  = e.getMessage().substring(7);
                    message = message.substring(0,message.length()-1);
                    ServerHttpResponse response = exchange.getResponse();
                    HttpHeaders headers = response.getHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().writeWith(Flux.just(new DefaultDataBufferFactory().wrap(message.getBytes())));
                }
            }
        log.info("--STOP validate Token");
        log.info("--STOP GatewayFilter");
        return chain.filter(exchange);

    });
    }

    public static class Config {}
}