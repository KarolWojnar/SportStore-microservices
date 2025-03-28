package com.shop.customer.service;

import com.shop.customer.model.ActivationType;
import com.shop.customer.model.entity.Activation;
import com.shop.customer.model.entity.User;
import com.shop.customer.model.dto.UserDto;
import com.shop.customer.exception.UserException;
import com.shop.customer.repository.ActivationRepository;
import com.shop.customer.repository.CustomerRepository;
import com.shop.customer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ActivationRepository activationRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRepository customerRepository;

    @Transactional
    public UserDto createUser(UserDto user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UserException("Email already exists.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User newUser = userRepository.save(UserDto.toUserEntity(user));
        Activation activation = activationRepository.save(new Activation(newUser, ActivationType.REGISTRATION));
        //todo: notification-service: send email
        if (user.getFirstName() != null || user.getLastName() != null || user.getShippingAddress() != null) {
            return UserDto.toCustomerDto(customerRepository.save(UserDto.toCustomerEntity(user, newUser)), newUser);
        }
        log.info("User created: {}", newUser.getId());
        return UserDto.toUserDto(newUser);
    }
}
