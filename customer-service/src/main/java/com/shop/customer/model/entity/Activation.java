package com.shop.customer.model.entity;

import com.shop.customer.model.ActivationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity(name = "activation")
public class Activation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Column(unique = true)
    private String activationCode;
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
    @Enumerated(EnumType.STRING)
    private ActivationType type;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public Activation(User user, ActivationType type) {
        this.user = user;
        this.type = type;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plusDays(2);
        this.activationCode = String.valueOf(UUID.randomUUID());
    }

}
