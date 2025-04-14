package com.shop.customer.service;

import com.shop.customer.model.dto.UserInfoDto;
import com.shop.customer.model.dto.UserInfoRequest;
import com.shop.customer.model.dto.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventService {


    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<String, CompletableFuture<UserInfoDto>> userInfoChecks = new ConcurrentHashMap<>();

    public CompletableFuture<UserInfoDto> getUserInfoRequest(String userId) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<UserInfoDto> future = new CompletableFuture<>();
        userInfoChecks.put(correlationId, future);

        try {
            UserInfoRequest request = new UserInfoRequest(userId, correlationId);
            kafkaTemplate.send("user-info-request", request);
            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
            executorService.schedule(() -> {
                if (!future.isDone()) {
                    future.completeExceptionally(new RuntimeException("Timeout waiting for user info response"));
                }
            }, 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.completeExceptionally(e);
            userInfoChecks.remove(correlationId);
        }
        return future;
    }

    @KafkaListener(topics = "user-info-response", groupId = "customer-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void userInfoResponseListener(UserInfoResponse userInfoResponse) {
        log.info("Received user info response: {}", userInfoResponse.getCorrelationId());
        CompletableFuture<UserInfoDto> future = userInfoChecks.remove(userInfoResponse.getCorrelationId());
        if (future != null) {
            future.complete(userInfoResponse.getUserInfoDto());
        }
    }
}
