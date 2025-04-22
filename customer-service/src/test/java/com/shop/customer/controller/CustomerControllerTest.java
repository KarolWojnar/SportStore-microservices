package com.shop.customer.controller;

import com.shop.customer.exception.CustomerException;
import com.shop.customer.model.ShippingAddress;
import com.shop.customer.model.dto.CustomerDto;
import com.shop.customer.model.dto.UserDetailsDto;
import com.shop.customer.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private CustomerContoller customerController;

    private final String userId = "123";
    private final ShippingAddress shippingAddress = new ShippingAddress("123 Main St", "Ny", "USA","01100");
    private final CustomerDto customerDto = new CustomerDto("John", "Doe", shippingAddress);
    private final UserDetailsDto userDetailsDto = new UserDetailsDto("John", "Doe", shippingAddress);

    @Test
    void getCustomer_shouldReturnUserDetails() {
        when(customerService.getUserDetails(anyString())).thenReturn(userDetailsDto);

        ResponseEntity<?> response = customerController.getCustomer(userId);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(userDetailsDto, response.getBody());
        verify(customerService).getUserDetails(userId);
    }

    @Test
    void getCustomer_shouldHandleServiceException() {
        when(customerService.getUserDetails(anyString()))
                .thenThrow(new CustomerException("Error getting user details"));

        assertThrows(CustomerException.class, () -> {
            customerController.getCustomer(userId);
        });
    }

    @Test
    void updateCustomer_shouldUpdateAndReturnUserDetails() {
        when(customerService.updateCustomer(any(CustomerDto.class), anyString()))
                .thenReturn(userDetailsDto);

        ResponseEntity<?> response = customerController.updateCustomer(customerDto, userId);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(userDetailsDto, response.getBody());
        verify(customerService).updateCustomer(customerDto, userId);
    }

    @Test
    void updateCustomer_shouldHandleServiceException() {
        when(customerService.updateCustomer(any(CustomerDto.class), anyString()))
                .thenThrow(new CustomerException("Error updating customer"));

        assertThrows(CustomerException.class, () -> {
            customerController.updateCustomer(customerDto, userId);
        });
    }

    @Test
    void updateCustomer_shouldValidateNonNullUserId() {
        assertThrows(NullPointerException.class, () -> {
            customerController.updateCustomer(customerDto, null);
        });
    }

    @Test
    void getCustomer_shouldValidateNonNullUserId() {
        assertThrows(NullPointerException.class, () -> customerController.getCustomer(null));
    }
}