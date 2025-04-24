package com.shop.authservice.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.authservice.model.dto.AuthUser;
import com.shop.authservice.model.dto.ResetPassword;
import com.shop.authservice.model.dto.UserDto;
import com.shop.authservice.model.entity.Activation;
import com.shop.authservice.repository.ActivationRepository;
import com.shop.authservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ActivationRepository activationRepository;

    @Test
    void shouldCreateUser() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setEmail("test@example.com");
        userDto.setPassword("password123");
        userDto.setConfirmPassword("password123");
        when(userService.createUser(any())).thenReturn(userDto);

        mockMvc.perform(post("/api/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void shouldLoginUser() throws Exception {
        AuthUser authUser = new AuthUser("test@example.com", "password");
        when(userService.login(any(), any())).thenReturn(Map.of("token", "abc123", "cartHasItems", true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("abc123"));
    }

    @Test
    void shouldStartPasswordRecovery() throws Exception {
        when(userService.recoveryPassword("test@example.com")).thenReturn("Code sent! Check your email.");

        mockMvc.perform(post("/api/auth/recovery-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Code sent! Check your email."));
    }

    @Test
    void shouldCheckResetCode() throws Exception {
        when(userService.checkResetCode("1234")).thenReturn(true);

        mockMvc.perform(get("/api/auth/check-reset-code/1234"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void shouldResetPassword() throws Exception {
        ResetPassword request = new ResetPassword();
        request.setCode("1234");
        request.setPassword("newpass");
        request.setConfirmPassword("newpass");
        Activation activation = new Activation();

        when(userService.resetPassword(any())).thenReturn("Password reset successfully.");
        when(activationRepository.findByActivationCodeAndTypeAndExpiresAtAfter(any(), any(), any())).thenReturn(Optional.of(activation));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully."));
    }
}