package com.shop.notificationservice.service;

import com.shop.notificationservice.model.dto.OrderSentEmailDto;
import com.shop.notificationservice.model.dto.ProductInfoEmail;
import com.shop.notificationservice.model.dto.UserDataOperationEvent;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private final String testEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        emailService.setSender(testEmail);
        String frontendUrl = "http://localhost:4200/";
        emailService.setUrl(frontendUrl);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void sendEmailActivation_shouldSendEmail() {
        UserDataOperationEvent event = new UserDataOperationEvent(
                testEmail,
                "activation-code",
                LocalDateTime.now().plusHours(24),
                LocalDateTime.now()
        );

        emailService.sendEmailActivation(event);

        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendEmailResetPassword_shouldSendEmail() {
        UserDataOperationEvent event = new UserDataOperationEvent(
                testEmail,
                "reset-code",
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now()
        );

        emailService.sendEmailResetPassword(event);

        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendEmailWithOrderDetails_shouldSendEmail() {
        OrderSentEmailDto order = createTestOrder();

        emailService.sendEmailWithOrderDetails(order);

        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void sendEmailToDelivered_shouldSendEmail() {
        OrderSentEmailDto order = createTestOrder();

        emailService.sendEmailToDelivered(order);

        verify(javaMailSender).createMimeMessage();
        verify(javaMailSender).send(mimeMessage);
    }

    private OrderSentEmailDto createTestOrder() {
        ProductInfoEmail product = new ProductInfoEmail(
                "prod-123", "Test Product", 2, new BigDecimal("19.99"), false
        );

        return new OrderSentEmailDto(
                "John",
                "Doe",
                testEmail,
                "order-123",
                new Date(),
                "123 Main St",
                "12345",
                "New York",
                "USA",
                new BigDecimal("39.98"),
                List.of(product)
        );
    }
}