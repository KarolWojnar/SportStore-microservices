package com.shop.authservice.repository;

import com.shop.authservice.model.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findAllBySentFalse();
    void deleteAllBySentTrueAndSentAtBefore(LocalDateTime time);
}
