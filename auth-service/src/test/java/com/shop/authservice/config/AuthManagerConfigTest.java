package com.shop.authservice.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthManagerConfigTest {

    @Mock
    private AuthenticationConfiguration authConfig;

    @InjectMocks
    private AuthManagerConfig authManagerConfig;

    @Test
    public void testAuthenticationProvider() {
        AuthenticationProvider provider = authManagerConfig.authenticationProvider();
        assertNotNull(provider);
        assertInstanceOf(DaoAuthenticationProvider.class, provider);
    }

    @Test
    public void testPasswordEncoder() {
        PasswordEncoder encoder = authManagerConfig.passwordEncoder();
        assertNotNull(encoder);
    }

    @Test
    public void testAuthenticationManager() throws Exception {
        AuthenticationManager mockManager = mock(AuthenticationManager.class);
        when(authConfig.getAuthenticationManager()).thenReturn(mockManager);

        AuthenticationManager manager = authManagerConfig.authenticationManager(authConfig);
        assertNotNull(manager);
        assertEquals(mockManager, manager);
    }
}