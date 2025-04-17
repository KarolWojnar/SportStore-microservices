package com.shop.customer.service;

import com.shop.customer.model.dto.*;
import com.shop.customer.model.entity.Customer;
import com.shop.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventService {

    private final CustomerRepository customerRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<String, CompletableFuture<UserInfoDto>> userInfoChecks = new ConcurrentHashMap<>();

    public CompletableFuture<UserInfoDto> getUserInfoRequest(String userId) {
        String correlationId = UUID.randomUUID().toString();
        CompletableFuture<UserInfoDto> future = new CompletableFuture<>();
        userInfoChecks.put(correlationId, future);

        try {
            UserInfoRequest request = new UserInfoRequest(userId, correlationId);
            kafkaTemplate.send("user-info-request", request);
            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
            executorService.schedule(() -> {
                if (!future.isDone()) {
                    future.completeExceptionally(new RuntimeException("Timeout waiting for user info response"));
                }
            }, 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            future.completeExceptionally(e);
            userInfoChecks.remove(correlationId);
        }
        return future;
    }

    @KafkaListener(topics = "user-info-response", groupId = "customer-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void userInfoResponseListener(UserInfoResponse userInfoResponse) {
        log.info("Received user info response: {}", userInfoResponse.getCorrelationId());
        CompletableFuture<UserInfoDto> future = userInfoChecks.remove(userInfoResponse.getCorrelationId());
        if (future != null) {
            future.complete(userInfoResponse.getUserInfoDto());
        }
    }

    @KafkaListener(topics = "customer-info-request", groupId = "customer-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void customerInfoRequestListener(CustomerInfoRequest customerInfoRequest) {
        log.info("Received customer info request: {}", customerInfoRequest.getUserId());
        Customer customer = customerRepository.findByUserId(Long.valueOf(customerInfoRequest.getUserId())).orElse(null);
        CustomerInfoResponse response = new CustomerInfoResponse(customerInfoRequest.getCorrelationId(), CustomerDto.toDto(customer));
        kafkaTemplate.send("customer-info-response", response);
    }

    @KafkaListener(topics = "user-customer-info-request", groupId = "customer-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void customerInfoResponseListener(UserCustomerInfoRequest request) {
        log.info("Received customer info response: {}", request.getCorrelationId());
        List<Customer> customer = customerRepository.findAllById(request.getUserIds());
        List<UserCustomerDto> customerDto = customer.stream().map(UserCustomerDto::toDto).toList();
        UserCustomerInfoResponse response = new UserCustomerInfoResponse(request.getCorrelationId(), customerDto);
        kafkaTemplate.send("user-customer-info-response", response);
    }

    @Transactional
    @KafkaListener(topics = "customer-order-request", groupId = "customer-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void customerOrderRequestListener(CustomerCreateRequest request) {
        log.info("Received customer order request: {}", request.getCorrelationId());
        CustomerFromOrderDto customerDto = request.getCustomerFromOrderDto();
        Customer customer = customerRepository.findByUserId(customerDto.getUserId())
                .orElseGet(Customer::new);
        customer.setUserId(customerDto.getUserId());
        customer.setFirstName(customerDto.getFirstName());
        customer.setLastName(customerDto.getLastName());
        customer.setShippingAddress(customerDto.getAddress());
        customerRepository.saveAndFlush(customer);
        kafkaTemplate.send("customer-order-response", request.getCorrelationId());
    }
}
