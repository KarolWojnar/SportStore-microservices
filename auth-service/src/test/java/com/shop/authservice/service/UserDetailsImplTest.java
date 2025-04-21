package com.shop.authservice.service;

import com.shop.authservice.model.Roles;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class UserDetailsImplTest {

    @Test
    public void testUserDetailsImpl() {
        String email = "test@example.com";
        String password = "password";
        Roles role = Roles.ROLE_CUSTOMER;
        boolean enabled = true;

        UserDetailsImpl userDetails = new UserDetailsImpl(email, password, role, enabled);

        assertEquals(email, userDetails.getUsername());
        assertEquals(password, userDetails.getPassword());
        assertTrue(userDetails.isEnabled());

        Collection<? extends SimpleGrantedAuthority> authorities = (Collection<? extends SimpleGrantedAuthority>) userDetails.getAuthorities();
        assertEquals(1, authorities.size());
        assertEquals(role.name(), authorities.iterator().next().getAuthority());
    }
}