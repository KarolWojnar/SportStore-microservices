package com.shop.authservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.authservice.model.dto.*;
import com.shop.authservice.model.entity.Activation;
import com.shop.authservice.model.entity.OutboxEvent;
import com.shop.authservice.model.entity.User;
import com.shop.authservice.repository.OutboxEventRepository;
import com.shop.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventService {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<String, CompletableFuture<Boolean>> pendingCartChecks = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<List<UserCustomerDto>>> customersInfoRequests = new ConcurrentHashMap<>();
    private final OutboxEventRepository outboxEventRepository;

    public void sendRegistrationEvent(String email, Activation activation) {
        try {
            UserDataOperationEvent register = new UserDataOperationEvent(
                    email,
                    activation.getActivationCode(),
                    activation.getExpiresAt(),
                    LocalDateTime.now()
            );

            String json = objectMapper.writeValueAsString(register);
            OutboxEvent event = new OutboxEvent(
                    "user-registration-events",
                    json,
                    register.getClass().getName()
            );
            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    public void sendPasswordResetEvent(String email, Activation activation) {
        try {
            UserDataOperationEvent register = new UserDataOperationEvent(
                    email,
                    activation.getActivationCode(),
                    activation.getExpiresAt(),
                    LocalDateTime.now()
            );
            String json = objectMapper.writeValueAsString(register);
            OutboxEvent event = new OutboxEvent(
                    "user-password-reset-events",
                    json,
                    register.getClass().getName()
            );

            outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    @Scheduled(cron = "*/5 * * * * *")
    public void trySendEvents() {
        List<OutboxEvent> events = outboxEventRepository.findAllBySentFalse();
        events.forEach(event -> {
            try {
                Object mappedClass = objectMapper
                        .readValue(event.getPayload(), Class.forName(event.getEventType()));
                kafkaTemplate.send(event.getTopic(), mappedClass);
                event.setSent(true);
                event.setSentAt(LocalDateTime.now());
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Error sending event: {}", event.getTopic(), e);
            }
        });
        if (!events.isEmpty()) {
            log.info("Sent {} events", events.size());
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void clearEvents() {
        log.info("Clearing events");
        LocalDateTime time = LocalDateTime.now().minusDays(2);
        outboxEventRepository.deleteAllBySentTrueAndSentAtBefore(time);
        outboxEventRepository.deleteAll();
    }

    public CompletableFuture<Boolean> checkCartNotEmptyRequest(Long userId) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingCartChecks.put(correlationId, future);

        try {
            CartInfoRequest cartInfoRequest = new CartInfoRequest(correlationId, String.valueOf(userId));
            kafkaTemplate.send("cart-items-request", cartInfoRequest);
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            executor.schedule(() -> {
                if (!future.isDone()) {
                    future.completeExceptionally(new TimeoutException("Request timed out"));
                    pendingCartChecks.remove(correlationId);
                }
            }, 2, TimeUnit.SECONDS);
            log.info("Sent cart check request for user: {}", userId);
        } catch (Exception e) {
            future.completeExceptionally(e);
            pendingCartChecks.remove(correlationId);
        }
        return future;
    }

    @KafkaListener(topics = "cart-items-response", groupId = "auth-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void checkCartNotEmptyResponse(CartInfoResponse response) {
        CompletableFuture<Boolean> future = pendingCartChecks.remove(response.getCorrelationId());
        if (future != null) {
            future.complete(response.isCartHasItems());
        }
    }

    @KafkaListener(topics = "user-info-request", groupId = "auth-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void getUserInfoResponse(UserInfoRequest response) {
        try {
            User user = userRepository.findById(Long.valueOf(response.getUserId())).orElse(null);
            UserInfoDto userInfoDto = new UserInfoDto();
            if (user != null) {
                userInfoDto = UserInfoDto.mapToDto(user);
            }
            UserInfoResponse userInfoResponse = new UserInfoResponse(userInfoDto, response.getCorrelationId());
            kafkaTemplate.send("user-info-response", userInfoResponse);
        } catch (Exception e) {
            log.error("Error processing user info request", e);
        }
    }

    @KafkaListener(topics = "user-email-request", groupId = "auth-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void getUserInfoResponse(UserEmailRequest request) {
        User user = userRepository.findById(request.getUserId()).orElse(null);
        UserEmailResponse userEmailResponse = new UserEmailResponse();
        userEmailResponse.setCorrelationId(request.getCorrelationId());
        if (user == null) {
            userEmailResponse.setEmail(null);
            userEmailResponse.setErrorMessage("User not found");
            kafkaTemplate.send("user-email-response", userEmailResponse);
            return;
        }
        userEmailResponse.setEmail(user.getEmail());
        kafkaTemplate.send("user-email-response", userEmailResponse);
    }

    public CompletableFuture<List<UserCustomerDto>> getCustomerByUserIds(List<Long> list) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<List<UserCustomerDto>> future = new CompletableFuture<>();
        customersInfoRequests.put(correlationId, future);
        try {
            UserCustomerInfoRequest customerInfoRequest = new UserCustomerInfoRequest(correlationId, list);
            kafkaTemplate.send("user-customer-info-request", customerInfoRequest);
        } catch (Exception e) {
            future.completeExceptionally(e);
            customersInfoRequests.remove(correlationId);
        }
        return future;
    }

    @KafkaListener(topics = "user-customer-info-response", groupId = "auth-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void getCustomerInfoResponse(UserCustomerInfoResponse response) {
        CompletableFuture<List<UserCustomerDto>> future = customersInfoRequests.remove(response.getCorrelationId());
        if (future != null) {
            future.complete(response.getCustomers());
        }
    }
}
