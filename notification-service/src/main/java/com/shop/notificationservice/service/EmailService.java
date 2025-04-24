package com.shop.notificationservice.service;

import com.shop.notificationservice.model.dto.OrderSentEmailDto;
import com.shop.notificationservice.model.dto.ProductInfoEmail;
import com.shop.notificationservice.model.dto.UserDataOperationEvent;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

import static com.shop.notificationservice.service.ConstantStrings.ORDER_SUMMARY_EMAIL_BODY;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    @Setter
    @Value("${spring.mail.username}")
    private String sender;

    @Setter
    @Value("${frontend.url}")
    private String url;

    private final JavaMailSender javaMailSender;

    @Retryable(
            maxAttempts = 5,
            backoff = @Backoff(delay = 5000)
    )
    public void sendEmailActivation(UserDataOperationEvent registrationEvent) {
        try {
            String expiration = registrationEvent.getExpiresAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            String urlLink = url + "activate/" + registrationEvent.getActivationCode();
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(sender);
            helper.setTo(registrationEvent.getEmail());
            helper.setSubject(ConstantStrings.ACTIVATION_EMAIL_SUBJECT);
            helper.setText(ConstantStrings.ACTIVATION_EMAIL_BODY
                    .formatted(urlLink, expiration, urlLink, urlLink), true);
            javaMailSender.send(message);
            log.info("Activation email sent to {}", registrationEvent.getEmail());
        } catch (Exception e) {
            log.error("Error sending activation email email to {}", registrationEvent.getEmail(), e);
            throw new RuntimeException("Failed to send activation email", e);
        }
    }

    @Retryable(
            maxAttempts = 5,
            backoff = @Backoff(delay = 5000)
    )
    public void sendEmailResetPassword(UserDataOperationEvent resetPassword) {
        try {
            String expiration = resetPassword.getExpiresAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            String urlLink = url + "reset-password/" + resetPassword.getActivationCode();
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(sender);
            helper.setTo(resetPassword.getEmail());
            helper.setSubject(ConstantStrings.RESET_PASSWORD_SUBJECT);
            helper.setText(ConstantStrings.RESET_PASSWORD_BODY
                    .formatted(urlLink, expiration, urlLink, urlLink), true);
            javaMailSender.send(message);
            log.info("Reset password email sent to {}", resetPassword.getEmail());
        } catch (Exception e) {
            log.error("Error sending reset password email to {}", resetPassword.getEmail(), e);
            throw new RuntimeException("Failed to send reset password email", e);
        }
    }

    public void sendEmailWithOrderDetails(OrderSentEmailDto order) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(sender);
            helper.setTo(order.getEmail());
            helper.setSubject(ConstantStrings.ORDER_EMAIL_SUBJECT);
            String emailBody = String.format(ConstantStrings.ORDER_EMAIL_BODY,
                    order.getFirstName(),
                    order.getOrderId(),
                    order.getOrderDate(),
                    order.getTotalPrice(),
                    order.getFirstName(),
                    order.getLastName(),
                    order.getAddress(),
                    order.getCity(),
                    order.getZipCode(),
                    order.getCountry(),
                    url + "profile",
                    url + "profile/orders/" + order.getOrderId()
            );
            helper.setText(emailBody, true);
            javaMailSender.send(message);
            log.info("Order email sent to {}", order.getEmail());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send order email", e);
        }
    }

    public void sendEmailToDelivered(OrderSentEmailDto order) {
        StringBuilder itemsHtml = getStringBuilder(order);
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(sender);
            helper.setTo(order.getEmail());
            helper.setSubject(ConstantStrings.ORDER_SUMMARY_SUBJECT);
            String emailBody = getString(order, itemsHtml);
            helper.setText(emailBody, true);
            javaMailSender.send(message);
            log.info("Delivered email sent to {}", order.getEmail());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send delivered email", e);
        }
    }

    private static StringBuilder getStringBuilder(OrderSentEmailDto order) {
        StringBuilder itemsHtml = new StringBuilder();
        for (ProductInfoEmail item : order.getProducts()) {
            itemsHtml.append(String.format("""
                <tr>
                    <td style="padding: 10px; border-bottom: 1px solid #ddd;">%s</td>
                    <td style="padding: 10px; border-bottom: 1px solid #ddd;">%d</td>
                    <td style="padding: 10px; border-bottom: 1px solid #ddd;">%.2f€</td>
                </tr>
                """, item.getName(), item.getAmount(), item.getPrice()));
        }
        return itemsHtml;
    }

    private String getString(OrderSentEmailDto order, StringBuilder itemsHtml) {
        String urlOrder = url + "profile/orders/" + order.getOrderId();
        String emailTemplate = ORDER_SUMMARY_EMAIL_BODY.replace("%", "%%");

        emailTemplate = emailTemplate
                .replace("%%s", "%s")
                .replace("%%n", "%n");
        return String.format(emailTemplate,
                order.getFirstName() + " " + order.getLastName(),
                order.getOrderId(),
                order.getOrderDate(),
                order.getTotalPrice(),
                order.getFirstName(),
                order.getLastName(),
                order.getAddress(),
                order.getCity(),
                order.getZipCode(),
                order.getCountry(),
                itemsHtml.toString(),
                urlOrder);
    }
}
