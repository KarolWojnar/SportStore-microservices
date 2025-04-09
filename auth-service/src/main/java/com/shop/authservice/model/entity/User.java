package com.shop.authservice.model.entity;

import com.shop.authservice.model.ProviderType;
import com.shop.authservice.model.Roles;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Entity(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Size(min = 8, message = "Password must be have at least 8 characters.")
    private String password;

    @Email(message = "Email is not valid.")
    @NotEmpty(message = "Email is required.")
    @Column(unique = true)
    private String email;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ProviderType provider = ProviderType.LOCAL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Roles role = Roles.ROLE_CUSTOMER;
    @Builder.Default
    private boolean enabled = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Activation> activations;
}
