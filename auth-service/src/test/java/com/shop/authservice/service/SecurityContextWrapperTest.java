package com.shop.authservice.service;

import com.shop.authservice.model.entity.User;
import com.shop.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityContextWrapperTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SecurityContextWrapper securityContextWrapper;

    private final String testEmail = "test@example.com";
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(testEmail);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getCurrentUser_ShouldReturnUser_WhenUserIsAuthenticated() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(testEmail);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));

        Optional<User> result = securityContextWrapper.getCurrentUser();
        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(securityContext).getAuthentication();
        verify(authentication).getName();
        verify(userRepository).findByEmail(testEmail);
    }

    @Test
    void getCurrentUser_ShouldReturnEmpty_WhenUserNotFound() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(testEmail);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        Optional<User> result = securityContextWrapper.getCurrentUser();

        assertFalse(result.isPresent());
        verify(securityContext).getAuthentication();
        verify(authentication).getName();
        verify(userRepository).findByEmail(testEmail);
    }

    @Test
    void getCurrentUser_ShouldReturnEmpty_WhenExceptionOccurs() {
        when(securityContext.getAuthentication()).thenThrow(new RuntimeException("Test exception"));
        Optional<User> result = securityContextWrapper.getCurrentUser();

        assertFalse(result.isPresent());
        verify(securityContext).getAuthentication();
        verify(userRepository, never()).findByEmail(anyString());
    }
}