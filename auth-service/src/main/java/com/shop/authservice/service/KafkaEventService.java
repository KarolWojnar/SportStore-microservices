package com.shop.authservice.service;

import com.shop.authservice.model.dto.UserDataOperationEvent;
import com.shop.authservice.model.entity.Activation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendRegistrationEvent(String email, Activation activation) {
        UserDataOperationEvent register = new UserDataOperationEvent(
                email,
                activation.getActivationCode(),
                activation.getExpiresAt(),
                LocalDateTime.now()
        );

       kafkaTemplate.send("user-registration-events", register);
    }

    public void sendPasswordResetEvent(String email, Activation activation) {
        UserDataOperationEvent register = new UserDataOperationEvent(
                email,
                activation.getActivationCode(),
                activation.getExpiresAt(),
                LocalDateTime.now()
        );

        kafkaTemplate.send("user-password-reset-events", register);
    }
}
