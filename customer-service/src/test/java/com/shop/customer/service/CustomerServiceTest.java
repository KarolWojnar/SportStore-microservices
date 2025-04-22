package com.shop.customer.service;

import com.shop.customer.exception.CustomerException;
import com.shop.customer.model.ShippingAddress;
import com.shop.customer.model.dto.CustomerDto;
import com.shop.customer.model.dto.UserDetailsDto;
import com.shop.customer.model.dto.UserInfoDto;
import com.shop.customer.model.entity.Customer;
import com.shop.customer.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private KafkaEventService kafkaEventService;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void updateCustomer_shouldCreateNewCustomerWhenNotExists() {
        String userId = "123";
        ShippingAddress shippingAddress = new ShippingAddress("Street 77", "Los Santos", "USA", "99-123");
        CustomerDto customerDto = new CustomerDto("John", "Doe", shippingAddress);
        UserInfoDto userInfoDto = new UserInfoDto();

        when(customerRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(kafkaEventService.getUserInfoRequest(anyString()))
                .thenReturn(CompletableFuture.completedFuture(userInfoDto));
        Object result = customerService.updateCustomer(customerDto, userId);

        assertInstanceOf(UserDetailsDto.class, result);
        UserDetailsDto detailsDto = (UserDetailsDto) result;
        assertEquals("John", detailsDto.getFirstName());
        assertEquals("Doe", detailsDto.getLastName());

        verify(customerRepository).save(argThat(c ->
                c.getUserId() == 123L &&
                        c.getFirstName().equals("John") &&
                        c.getLastName().equals("Doe")
        ));
    }

    @Test
    void updateCustomer_shouldUpdateExistingCustomer() {
        String userId = "123";
        ShippingAddress shippingAddress = new ShippingAddress("Street 77", "Los Santos", "USA", "99-123");
        CustomerDto customerDto = new CustomerDto("John", "Doe", shippingAddress);
        ShippingAddress oldShippingAddress = new ShippingAddress("Street 772", "Los Santos", "USA", "92-123");
        Customer existingCustomer = new Customer(123L, 123L, "John", "Doe", oldShippingAddress);
        UserInfoDto userInfoDto = new UserInfoDto();

        when(customerRepository.findByUserId(anyLong())).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(kafkaEventService.getUserInfoRequest(anyString()))
                .thenReturn(CompletableFuture.completedFuture(userInfoDto));

        Object result = customerService.updateCustomer(customerDto, userId);

        assertInstanceOf(UserDetailsDto.class, result);
        UserDetailsDto detailsDto = (UserDetailsDto) result;
        assertEquals("John", detailsDto.getFirstName());
        assertEquals("Doe", detailsDto.getLastName());

        verify(customerRepository).save(argThat(c ->
                c.getUserId() == 123L &&
                        c.getFirstName().equals("John") &&
                        c.getLastName().equals("Doe")
        ));
    }

    @Test
    void updateCustomer_shouldThrowExceptionOnError() {
        String userId = "123";
        ShippingAddress shippingAddress = new ShippingAddress("Street 77", "Los Santos", "USA", "99-123");
        CustomerDto customerDto = new CustomerDto("John", "Doe", shippingAddress);

        when(customerRepository.findByUserId(anyLong())).thenThrow(new RuntimeException("DB error"));

        assertThrows(CustomerException.class, () -> customerService.updateCustomer(customerDto, userId));
    }

    @Test
    void getUserDetails_shouldReturnUserDetails() {
        String userId = "123";
        ShippingAddress shippingAddress = new ShippingAddress("Street 77", "Los Santos", "USA", "99-123");
        Customer customer = new Customer(123L, 123L, "John", "Doe", shippingAddress);
        UserInfoDto userInfoDto = new UserInfoDto();

        when(customerRepository.findByUserId(anyLong())).thenReturn(Optional.of(customer));
        when(kafkaEventService.getUserInfoRequest(anyString()))
                .thenReturn(CompletableFuture.completedFuture(userInfoDto));

        UserDetailsDto result = customerService.getUserDetails(userId);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
    }

    @Test
    void getUserDetails_shouldThrowExceptionOnError() {
        String userId = "123";

        when(customerRepository.findByUserId(anyLong())).thenThrow(new RuntimeException("DB error"));

        assertThrows(CustomerException.class, () -> customerService.getUserDetails(userId));
    }
}