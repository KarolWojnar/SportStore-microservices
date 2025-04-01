package com.shop.authservice.repository;

import com.shop.authservice.model.ActivationType;
import com.shop.authservice.model.entity.Activation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ActivationRepository extends JpaRepository<Activation, Long> {
    Optional<Activation> findByActivationCodeAndTypeAndExpiresAtAfter(String activationCode, ActivationType type, LocalDateTime expiresAt);

}
