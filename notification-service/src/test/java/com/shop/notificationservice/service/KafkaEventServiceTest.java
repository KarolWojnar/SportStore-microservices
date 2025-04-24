package com.shop.notificationservice.service;

import com.shop.notificationservice.model.dto.OrderSentEmailDto;
import com.shop.notificationservice.model.dto.UserDataOperationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventServiceTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private KafkaEventService kafkaEventService;

    @Test
    void handleRegistrationEvent_shouldCallEmailService() {
        UserDataOperationEvent event = new UserDataOperationEvent(
                "test@example.com",
                "activation-code",
                LocalDateTime.now().plusHours(24),
                LocalDateTime.now()
        );

        kafkaEventService.handleRegistrationEvent(event);

        verify(emailService).sendEmailActivation(event);
    }

    @Test
    void handleResetPasswordEvent_shouldCallEmailService() {
        UserDataOperationEvent event = new UserDataOperationEvent(
                "test@example.com",
                "reset-code",
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now()
        );

        kafkaEventService.handleResetPasswordEvent(event);

        verify(emailService).sendEmailResetPassword(event);
    }

    @Test
    void handleOrderSentEvent_shouldCallEmailService() {
        OrderSentEmailDto order = new OrderSentEmailDto();
        order.setEmail("test@example.com");

        kafkaEventService.handleOrderSentEvent(order);

        verify(emailService).sendEmailWithOrderDetails(order);
    }

    @Test
    void handleOrderDeliveredListener_shouldCallEmailService() {
        OrderSentEmailDto order = new OrderSentEmailDto();
        order.setEmail("test@example.com");

        kafkaEventService.handleOrderDeliveredListener(order);

        verify(emailService).sendEmailToDelivered(order);
    }
}