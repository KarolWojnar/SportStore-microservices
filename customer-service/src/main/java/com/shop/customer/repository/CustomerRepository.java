package com.shop.customer.repository;

import com.shop.customer.model.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUserId(Long userId);
    List<Customer> findByUserIdIn(List<Long> userIds);
}
