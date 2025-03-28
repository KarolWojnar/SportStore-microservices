package com.shop.customer.repository;

import com.shop.customer.model.entity.Activation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivationRepository extends JpaRepository<Activation, Long> {
}
