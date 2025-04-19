package com.shop.authservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.authservice.model.dto.*;
import com.shop.authservice.model.entity.Activation;
import com.shop.authservice.model.entity.OutboxEvent;
import com.shop.authservice.model.entity.User;
import com.shop.authservice.repository.OutboxEventRepository;
import com.shop.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private KafkaEventService kafkaEventService;

    private final String testEmail = "test@example.com";
    private final String testCode = "test-code";
    private final Long testUserId = 1L;

    @Test
    void sendRegistrationEvent_ShouldSaveOutboxEvent() throws JsonProcessingException {
        Activation activation = new Activation();
        activation.setActivationCode(testCode);
        activation.setExpiresAt(LocalDateTime.now().plusDays(1));

        UserDataOperationEvent expectedEvent = new UserDataOperationEvent(
                testEmail, testCode, activation.getExpiresAt(), LocalDateTime.now());

        when(objectMapper.writeValueAsString(any())).thenReturn("json-string");

        kafkaEventService.sendRegistrationEvent(testEmail, activation);

        verify(outboxEventRepository).save(argThat(event ->
                event.getTopic().equals("user-registration-events") &&
                        event.getEventType().equals(UserDataOperationEvent.class.getName())
        ));
    }

    @Test
    void sendPasswordResetEvent_ShouldSaveOutboxEvent() throws JsonProcessingException {
        Activation activation = new Activation();
        activation.setActivationCode(testCode);
        activation.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(objectMapper.writeValueAsString(any())).thenReturn("json-string");

        kafkaEventService.sendPasswordResetEvent(testEmail, activation);
        verify(outboxEventRepository).save(argThat(event ->
                event.getTopic().equals("user-password-reset-events") &&
                        event.getEventType().equals(UserDataOperationEvent.class.getName())
        ));
    }

    @Test
    @Transactional
    void trySendEvents_ShouldSendPendingEvents() throws Exception {
        OutboxEvent event = new OutboxEvent();
        event.setTopic("test-topic");
        event.setPayload("payload");
        event.setEventType(UserDataOperationEvent.class.getName());
        event.setSent(false);

        when(outboxEventRepository.findAllBySentFalse()).thenReturn(List.of(event));
        when(objectMapper.readValue(anyString(), any(Class.class))).thenReturn(new Object());

        kafkaEventService.trySendEvents();

        verify(kafkaTemplate).send(eq("test-topic"), any());
        verify(outboxEventRepository).save(argThat(OutboxEvent::isSent));
    }

    @Test
    void checkCartNotEmptyRequest_ShouldReturnFutureAndSendRequest() {
        CompletableFuture<Boolean> future = kafkaEventService.checkCartNotEmptyRequest(testUserId);

        assertNotNull(future);
        verify(kafkaTemplate).send(eq("cart-items-request"), any(CartInfoRequest.class));
    }

    @Test
    void checkCartNotEmptyResponse_ShouldCompleteFuture() {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        getPendingCartChecks().put(correlationId, future);

        CartInfoResponse response = new CartInfoResponse();
        response.setCorrelationId(correlationId);
        response.setCartHasItems(true);

        kafkaEventService.checkCartNotEmptyResponse(response);

        assertTrue(future.isDone(), "Future should be completed");
        assertTrue(future.join(), "Future should return true");
    }

    @Test
    void getUserInfoResponse_ShouldSendResponseWithUserData() {
        User user = new User();
        user.setId(testUserId);
        user.setEmail(testEmail);

        UserInfoRequest request = new UserInfoRequest();
        request.setUserId(String.valueOf(testUserId));
        request.setCorrelationId("corr-id");

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));

        kafkaEventService.getUserInfoResponse(request);

        verify(kafkaTemplate).send(eq("user-info-response"), any(UserInfoResponse.class));
    }

    @Test
    void getUserEmailResponse_ShouldSendResponseWithEmail() {
        User user = new User();
        user.setId(testUserId);
        user.setEmail(testEmail);

        UserEmailRequest request = new UserEmailRequest();
        request.setUserId(testUserId);
        request.setCorrelationId("corr-id");
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));

        kafkaEventService.getUserInfoResponse(request);

        verify(kafkaTemplate).send(eq("user-email-response"), any(UserEmailResponse.class));
    }

    @Test
    void getCustomerByUserIds_ShouldReturnFutureAndSendRequest() {
        CompletableFuture<List<UserCustomerDto>> future =
                kafkaEventService.getCustomerByUserIds(List.of(testUserId));

        assertNotNull(future);
        verify(kafkaTemplate).send(eq("user-customer-info-request"), any(UserCustomerInfoRequest.class));
    }

    @Test
    void getCustomerInfoResponse_ShouldCompleteFuture() {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<List<UserCustomerDto>> future = new CompletableFuture<>();
        getCustomersInfoRequests().put(correlationId, future);

        UserCustomerInfoResponse response = new UserCustomerInfoResponse();
        response.setCorrelationId(correlationId);
        response.setCustomers(List.of(new UserCustomerDto()));

        kafkaEventService.getCustomerInfoResponse(response);

        assertDoesNotThrow(() -> {
            List<UserCustomerDto> result = future.get(1, TimeUnit.SECONDS);
            assertEquals(1, result.size());
        });
    }

    @Test
    void clearEvents_ShouldDeleteOldEvents() {
        kafkaEventService.clearEvents();

        verify(outboxEventRepository).deleteAllBySentTrueAndSentAtBefore(any(LocalDateTime.class));
        verify(outboxEventRepository).deleteAll();
    }

    private Map<String, CompletableFuture<Boolean>> getPendingCartChecks() {
        try {
            var field = KafkaEventService.class.getDeclaredField("pendingCartChecks");
            field.setAccessible(true);
            return (Map<String, CompletableFuture<Boolean>>) field.get(kafkaEventService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, CompletableFuture<List<UserCustomerDto>>> getCustomersInfoRequests() {
        try {
            var field = KafkaEventService.class.getDeclaredField("customersInfoRequests");
            field.setAccessible(true);
            return (Map<String, CompletableFuture<List<UserCustomerDto>>>) field.get(kafkaEventService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}