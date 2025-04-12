package com.shop.notificationservice.service;

import com.shop.notificationservice.model.dto.UserDataOperationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventService {

    private final EmailService emailService;

    @KafkaListener(topics = "user-registration-events", groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleRegistrationEvent(UserDataOperationEvent registrationEvent) {
        log.info("Received registration event for email: {}", registrationEvent.getEmail());
        emailService.sendEmailActivation(registrationEvent);
    }

    @KafkaListener(topics = "user-password-reset-events", groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleResetPasswordEvent(UserDataOperationEvent registrationEvent) {
        log.info("Received reset password event for email: {}", registrationEvent.getEmail());
        emailService.sendEmailResetPassword(registrationEvent);
    }
}
