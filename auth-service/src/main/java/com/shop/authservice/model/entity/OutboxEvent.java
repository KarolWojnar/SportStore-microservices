package com.shop.authservice.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "outbox_event")
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String topic;
    private String eventType;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String payload;
    private boolean sent = false;
    private LocalDateTime createdAt = LocalDateTime.now();

    public OutboxEvent(String topic, String payload, String eventType) {
        this.topic = topic;
        this.eventType = eventType;
        this.payload = payload;
    }
}
