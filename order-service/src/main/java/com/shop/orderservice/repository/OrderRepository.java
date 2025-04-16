package com.shop.orderservice.repository;

import com.shop.orderservice.model.OrderStatus;
import com.shop.orderservice.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"products"})
    List<Order> findAllByStatusIsNotAndLastModifiedBefore(OrderStatus status, Date lastModified);
    @EntityGraph(attributePaths = {"products"})
    Optional<Order> findBySessionId(String sessionId);
    List<Order> findAllByUserId(String userId);
    Optional<Order> findByIdAndUserId(Long id, String id1);
    List<Order> findAllByStatusAndOrderDateBefore(OrderStatus status, Date orderDate);

    Page<Order> findAllByStatus(OrderStatus orderStatus, Pageable pageable);
}
