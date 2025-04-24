package com.shop.authservice.service;

import com.shop.authservice.model.entity.User;
import com.shop.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class SecurityContextWrapper {
    private final UserRepository userRepository;


    public Optional<User> getCurrentUser() {
        try {
            String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByEmail(userEmail);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
