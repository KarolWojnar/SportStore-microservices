package com.shop.customer.service;

import com.shop.customer.exception.CustomerException;
import com.shop.customer.model.dto.CustomerDto;
import com.shop.customer.model.dto.UserDetailsDto;
import com.shop.customer.model.dto.UserInfoDto;
import com.shop.customer.model.entity.Customer;
import com.shop.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final KafkaEventService kafkaEventService;

    public Object updateCustomer(CustomerDto customerDto, String userId) {
        try {
            Customer customer = customerRepository.findByUserId(Long.valueOf(userId)).orElse(null);
            if (customer == null) {
                customer = new Customer();
                customer.setUserId(Long.valueOf(userId));
            }
            customer.setFirstName(customerDto.getFirstName());
            customer.setLastName(customerDto.getLastName());
            customer.setShippingAddress(customerDto.getShippingAddress());
            customerRepository.save(customer);
            UserInfoDto userInfoDto = kafkaEventService.getUserInfoRequest(userId)
                    .get(5, TimeUnit.SECONDS);
            return UserDetailsDto.toDto(userInfoDto, customer);
        } catch (Exception e) {
            throw new CustomerException("Error while updating customer.");
        }
    }

    public UserDetailsDto getUserDetails(String userId) {
        try {
            Customer customer = customerRepository.findByUserId(Long.valueOf(userId)).orElse(null);
            UserInfoDto userInfoDto = kafkaEventService.getUserInfoRequest(userId)
                    .get(5, TimeUnit.SECONDS);
            return UserDetailsDto.toDto(userInfoDto, customer);
        } catch (Exception e) {
            throw new CustomerException("Error while getting info about user.");
        }
    }
}
