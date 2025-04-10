package com.shop.authservice.repository;

import com.shop.authservice.model.ProviderType;
import com.shop.authservice.model.Roles;
import com.shop.authservice.model.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByEmailAndEnabledAndProvider(String email, boolean enabled, ProviderType providerType);
    void deleteAllByIdIn(Collection<Long> id);
    @Query("SELECT u FROM users u WHERE " +
            "(?1 IS NULL OR LOWER(u.email) LIKE LOWER(?1)) AND " +
            "(?2 IS NULL OR u.role = ?2) AND " +
            "(?3 IS NULL OR u.enabled = ?3)")
    List<User> findByFilters(
            String email,
            Roles role,
            Boolean enabled,
            Pageable pageable);

    Optional<User> findByEmailAndProvider(String email, ProviderType providerType);
}
