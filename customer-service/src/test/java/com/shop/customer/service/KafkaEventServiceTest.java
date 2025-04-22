package com.shop.customer.service;

import com.shop.customer.model.ShippingAddress;
import com.shop.customer.model.dto.*;
import com.shop.customer.model.entity.Customer;
import com.shop.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaEventService kafkaEventService;

    private String userId;
    private String correlationId;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        userId = "123";
        correlationId = UUID.randomUUID().toString();
        ShippingAddress address = new ShippingAddress("123 Main St", "New York", "USA", "10001");
        testCustomer = new Customer(123L, 123L, "John", "Doe", address);
    }

    @Test
    void getUserInfoRequest_shouldSendRequestAndReturnFuture() {
        CompletableFuture<UserInfoDto> future = kafkaEventService.getUserInfoRequest(userId);

        assertNotNull(future);
        assertFalse(future.isDone());

        ArgumentCaptor<UserInfoRequest> captor = ArgumentCaptor.forClass(UserInfoRequest.class);
        verify(kafkaTemplate).send(eq("user-info-request"), captor.capture());

        UserInfoRequest request = captor.getValue();
        assertEquals(userId, request.getUserId());
        assertNotNull(request.getCorrelationId());
    }

    @Test
    void getUserInfoRequest_shouldHandleSendFailure() {
        when(kafkaTemplate.send(anyString(), any())).thenThrow(new RuntimeException("Kafka error"));

        CompletableFuture<UserInfoDto> future = kafkaEventService.getUserInfoRequest(userId);

        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    void customerInfoRequestListener_shouldSendResponseWithCustomerData() {
        CustomerInfoRequest request = new CustomerInfoRequest(correlationId, userId);
        when(customerRepository.findByUserId(123L)).thenReturn(Optional.of(testCustomer));

        kafkaEventService.customerInfoRequestListener(request);

        ArgumentCaptor<CustomerInfoResponse> captor = ArgumentCaptor.forClass(CustomerInfoResponse.class);
        verify(kafkaTemplate).send(eq("customer-info-response"), captor.capture());

        CustomerInfoResponse response = captor.getValue();
        assertEquals(correlationId, response.getCorrelationId());
        assertNotNull(response.getCustomer());
        assertEquals("John", response.getCustomer().getFirstName());
    }

    @Test
    void customerInfoRequestListener_shouldHandleNullCustomer() {
        CustomerInfoRequest request = new CustomerInfoRequest(correlationId, userId);
        when(customerRepository.findByUserId(123L)).thenReturn(Optional.empty());

        kafkaEventService.customerInfoRequestListener(request);

        ArgumentCaptor<CustomerInfoResponse> captor = ArgumentCaptor.forClass(CustomerInfoResponse.class);
        verify(kafkaTemplate).send(eq("customer-info-response"), captor.capture());

        CustomerInfoResponse response = captor.getValue();
        assertNull(response.getCustomer());
    }

    @Test
    void customerInfoResponseListener_shouldSendResponseWithCustomerList() {
        List<Long> userIds = List.of(123L, 456L);
        UserCustomerInfoRequest request = new UserCustomerInfoRequest(correlationId, userIds);
        when(customerRepository.findAllById(userIds)).thenReturn(List.of(testCustomer));

        kafkaEventService.customerInfoResponseListener(request);

        ArgumentCaptor<UserCustomerInfoResponse> captor = ArgumentCaptor.forClass(UserCustomerInfoResponse.class);
        verify(kafkaTemplate).send(eq("user-customer-info-response"), captor.capture());

        UserCustomerInfoResponse response = captor.getValue();
        assertEquals(correlationId, response.getCorrelationId());
        assertEquals(1, response.getCustomers().size());
        assertEquals("John", response.getCustomers().get(0).getFirstName());
    }

    @Test
    void customerOrderRequestListener_shouldCreateNewCustomer() {
        ShippingAddress address = new ShippingAddress("123 Main St", "New York", "USA", "10001");
        CustomerFromOrderDto customerDto = new CustomerFromOrderDto(123L, "John", "Doe", address);
        CustomerCreateRequest request = new CustomerCreateRequest(correlationId, customerDto);
        when(customerRepository.findByUserId(123L)).thenReturn(Optional.empty());
        when(customerRepository.saveAndFlush(any())).thenReturn(testCustomer);

        kafkaEventService.customerOrderRequestListener(request);

        verify(customerRepository).saveAndFlush(any(Customer.class));
        verify(kafkaTemplate).send(eq("customer-order-response"), eq(correlationId));
    }

    @Test
    void customerOrderRequestListener_shouldUpdateExistingCustomer() {
        ShippingAddress address = new ShippingAddress("123 Main St", "New York", "USA", "10001");
        CustomerFromOrderDto customerDto = new CustomerFromOrderDto(123L, "John", "Doe", address);
        CustomerCreateRequest request = new CustomerCreateRequest(correlationId, customerDto);
        when(customerRepository.findByUserId(123L)).thenReturn(Optional.of(testCustomer));
        when(customerRepository.saveAndFlush(any())).thenReturn(testCustomer);

        kafkaEventService.customerOrderRequestListener(request);

        verify(customerRepository).saveAndFlush(argThat(c ->
                c.getShippingAddress().equals(address)
        ));
        verify(kafkaTemplate).send(eq("customer-order-response"), eq(correlationId));
    }

    @Test
    void getUserInfoRequest_shouldTimeoutAfter5Seconds() throws Exception {
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        CompletableFuture<UserInfoDto> future = kafkaEventService.getUserInfoRequest(userId);

        Thread.sleep(5500);

        assertTrue(future.isCompletedExceptionally());
        future.exceptionally(ex -> {
            assertEquals("Timeout waiting for user info response", ex.getMessage());
            return null;
        });
    }
}