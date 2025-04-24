package com.shop.paymentservice.repository;

import com.shop.paymentservice.model.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findAllBySentFalse();
    void deleteAllBySentTrueAndSentAtBefore(LocalDateTime time);
}
