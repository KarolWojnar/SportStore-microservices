package com.shop.notificationservice.service;

import com.shop.notificationservice.model.dto.UserDataOperationEvent;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    @Value("${spring.mail.username}")
    private String sender;

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

//    public void sendEmailWithOrderDetails(Order order) {
//        User user = userRepository.findById(order.getUserId()).orElseThrow(() -> new UserException("User not found."));
//        Customer customer = customerRepository.findByUserId(user.getId()).orElseThrow(() -> new UserException("Customer not found."));
//        try {
//            MimeMessage message = javaMailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true);
//            helper.setFrom(sender);
//            helper.setTo(user.getEmail());
//            helper.setSubject(ConstantStrings.ORDER_EMAIL_SUBJECT);
//            String emailBody = String.format(ConstantStrings.ORDER_EMAIL_BODY,
//                    customer.getFirstName(),
//                    order.getId(),
//                    order.getOrderDate(),
//                    order.getTotalPrice(),
//                    customer.getFirstName(),
//                    customer.getLastName(),
//                    order.getOrderAddress().getAddress(),
//                    order.getOrderAddress().getCity(),
//                    order.getOrderAddress().getZipCode(),
//                    order.getOrderAddress().getCountry(),
//                    url + "profile",
//                    url + "profile/orders/" + order.getId()
//            );
//            helper.setText(emailBody, true);
//            javaMailSender.send(message);
//            log.info("Order email sent to {}", user.getEmail());
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to send order email", e);
//        }
//    }
//
//    public void sendEmailToDelivered(Order order, User user, Customer customer) {
//        StringBuilder itemsHtml = new StringBuilder();
//        for (ProductInOrder item : order.getProducts()) {
//            Optional<Product> product = productRepository.findById(item.getProductId());
//            product.ifPresent(value -> itemsHtml.append(String.format("""
//                    <tr>
//                        <td style="padding: 10px; border-bottom: 1px solid #ddd;">%s</td>
//                        <td style="padding: 10px; border-bottom: 1px solid #ddd;">%d</td>
//                        <td style="padding: 10px; border-bottom: 1px solid #ddd;">%.2f€</td>
//                    </tr>
//                    """, value.getName(), item.getAmount(), item.getPrice())));
//        }
//        try {
//            MimeMessage message = javaMailSender.createMimeMessage();
//            MimeMessageHelper helper = new MimeMessageHelper(message, true);
//            helper.setFrom(sender);
//            helper.setTo(user.getEmail());
//            helper.setSubject(ConstantStrings.ORDER_SUMMARY_SUBJECT);
//            String emailBody = getString(order, customer, itemsHtml);
//            helper.setText(emailBody, true);
//            javaMailSender.send(message);
//            log.info("Delivered email sent to {}", user.getEmail());
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to send delivered email", e);
//        }
//    }
//
//    private String getString(Order order, Customer customer, StringBuilder itemsHtml) {
//        String urlOrder = url + "profile/orders/" + order.getId();
//        String emailTemplate = ORDER_SUMMARY_EMAIL_BODY.replace("%", "%%");
//
//        emailTemplate = emailTemplate
//                .replace("%%s", "%s")
//                .replace("%%n", "%n");
//        return String.format(emailTemplate,
//                customer.getFirstName() + " " + customer.getLastName(),
//                order.getId(),
//                order.getOrderDate(),
//                order.getTotalPrice(),
//                customer.getFirstName(),
//                customer.getLastName(),
//                order.getOrderAddress().getAddress(),
//                order.getOrderAddress().getCity(),
//                order.getOrderAddress().getZipCode(),
//                order.getOrderAddress().getCountry(),
//                itemsHtml.toString(),
//                urlOrder);
//    }
}
