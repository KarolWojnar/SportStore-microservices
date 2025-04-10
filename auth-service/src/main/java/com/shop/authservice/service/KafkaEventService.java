package com.shop.authservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.authservice.model.dto.UserDataOperationEvent;
import com.shop.authservice.model.entity.Activation;
import com.shop.authservice.model.entity.OutboxEvent;
import com.shop.authservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventService {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
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

    @Scheduled(cron = "*/5 * * * * *")
    public void trySendEvents() {
        List<OutboxEvent> events = outboxEventRepository.findAllBySentFalse();
        events.forEach(event -> {
            try {
                Object mappedClass = objectMapper.readValue(event.getPayload(), Class.forName(event.getEventType()));
                kafkaTemplate.send(event.getTopic(), mappedClass);
                event.setSent(true);
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Error sending event: {}", event.getTopic(), e);
            }
        });
        log.info("Sent {} events", events.size());
    }
}
