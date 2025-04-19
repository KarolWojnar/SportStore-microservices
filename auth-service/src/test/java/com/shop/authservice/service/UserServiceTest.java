package com.shop.authservice.service;

import com.shop.authservice.exception.UserException;
import com.shop.authservice.model.ActivationType;
import com.shop.authservice.model.ProviderType;
import com.shop.authservice.model.Roles;
import com.shop.authservice.model.dto.AuthUser;
import com.shop.authservice.model.dto.UserDto;
import com.shop.authservice.model.entity.Activation;
import com.shop.authservice.model.entity.User;
import com.shop.authservice.repository.ActivationRepository;
import com.shop.authservice.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivationRepository activationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private KafkaEventService kafkaEventService;

    @Mock
    private JwtService jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDto testUserDto;
    private final String testPassword = "password";
    private final String testEmail = "test@example.com";
    private final String encodedPassword = "encodedPassword";
    private final String testToken = "test.jwt.token";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "exp", 3600000);
        ReflectionTestUtils.setField(userService, "refreshExp", 86400000);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(testEmail);
        testUser.setPassword(encodedPassword);
        testUser.setRole(Roles.ROLE_CUSTOMER);
        testUser.setEnabled(true);
        testUser.setProvider(ProviderType.LOCAL);

        testUserDto = new UserDto();
        testUserDto.setEmail(testEmail);
        testUserDto.setPassword(testPassword);
        testUserDto.setRole(Roles.ROLE_CUSTOMER);
    }

    @Test
    void createUser_ShouldCreateUser_WhenEmailDoesNotExist() {
        when(userRepository.existsByEmail(testEmail)).thenReturn(false);
        when(passwordEncoder.encode(testPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        Activation activation = new Activation();
        activation.setUser(testUser);
        activation.setActivationCode("code");
        activation.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(activationRepository.save(any(Activation.class))).thenReturn(activation);

        doNothing().when(kafkaEventService).sendRegistrationEvent(anyString(), any(Activation.class));
        UserDto result = userService.createUser(testUserDto);

        assertNotNull(result);
        assertEquals(testEmail, result.getEmail());
        verify(userRepository).existsByEmail(testEmail);
        verify(passwordEncoder).encode(testPassword);
        verify(userRepository).save(any(User.class));
        verify(activationRepository).save(any(Activation.class));
        verify(kafkaEventService).sendRegistrationEvent(eq(testEmail), any(Activation.class));
    }

    @Test
    void createUser_ShouldThrowException_WhenEmailExists() {
        when(userRepository.existsByEmail(testEmail)).thenReturn(true);

        assertThrows(UserException.class, () -> userService.createUser(testUserDto));
        verify(userRepository).existsByEmail(testEmail);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldReturnCredentials_WhenCredentialsAreValid() {
        AuthUser authUser = new AuthUser();
        authUser.setEmail(testEmail);
        authUser.setPassword(testPassword);

        when(userRepository.findByEmailAndProvider(testEmail, ProviderType.LOCAL))
                .thenReturn(Optional.of(testUser));

        when(jwtUtil.generateToken(testUser, 3600000)).thenReturn(testToken);
        String testRefreshToken = "test.refresh.token";
        when(jwtUtil.generateToken(testUser, 86400000)).thenReturn(testRefreshToken);

        when(authenticationManager.authenticate(any())).thenReturn(null);

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.complete(true);
        when(kafkaEventService.checkCartNotEmptyRequest(testUser.getId())).thenReturn(future);

        Map<String, Object> result = userService.login(authUser, response);

        assertNotNull(result);
        assertEquals(testToken, result.get("token"));
        assertEquals(true, result.get("cartHasItems"));

        verify(userRepository).findByEmailAndProvider(testEmail, ProviderType.LOCAL);
        verify(jwtUtil).generateToken(testUser, 3600000);
        verify(jwtUtil).generateToken(testUser, 86400000);
        verify(response).addCookie(any(Cookie.class));
        verify(kafkaEventService).checkCartNotEmptyRequest(testUser.getId());
    }

    @Test
    void login_ShouldThrowException_WhenEmailIsNull() {
        AuthUser authUser = new AuthUser();
        authUser.setPassword(testPassword);

        assertThrows(UserException.class, () -> userService.login(authUser, response));
        verify(userRepository, never()).findByEmailAndProvider(any(), any());
    }

    @Test
    void login_ShouldThrowException_WhenUserNotFound() {
        AuthUser authUser = new AuthUser();
        authUser.setEmail(testEmail);
        authUser.setPassword(testPassword);
        when(userRepository.findByEmailAndProvider(testEmail, ProviderType.LOCAL)).thenReturn(Optional.empty());

        assertThrows(UserException.class, () -> userService.login(authUser, response));
        verify(userRepository).findByEmailAndProvider(testEmail, ProviderType.LOCAL);
    }

    @Test
    void activate_ShouldActivateUser_WhenCodeIsValid() {
        String activationCode = "valid-code";
        LocalDateTime expiration = LocalDateTime.now().plusDays(1);

        Activation activation = new Activation();
        activation.setUser(testUser);
        activation.setActivationCode(activationCode);
        activation.setExpiresAt(expiration);
        activation.setType(ActivationType.REGISTRATION);

        when(activationRepository.findByActivationCodeAndTypeAndExpiresAtAfter(
                eq(activationCode), eq(ActivationType.REGISTRATION), any(LocalDateTime.class)))
                .thenReturn(Optional.of(activation));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        Map<String, String> result = userService.activate(activationCode);

        assertNotNull(result);
        assertEquals(testEmail, result.get("email"));
        assertTrue(testUser.isEnabled());

        verify(activationRepository).findByActivationCodeAndTypeAndExpiresAtAfter(
                eq(activationCode), eq(ActivationType.REGISTRATION), any(LocalDateTime.class));
        verify(userRepository).findById(testUser.getId());
        verify(userRepository).save(testUser);
        verify(activationRepository).delete(activation);
    }

    @Test
    void activate_ShouldThrowException_WhenCodeNotFound() {
        String activationCode = "invalid-code";
        when(activationRepository.findByActivationCodeAndTypeAndExpiresAtAfter(
                eq(activationCode), eq(ActivationType.REGISTRATION), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        assertThrows(UserException.class, () -> userService.activate(activationCode));

        verify(activationRepository).findByActivationCodeAndTypeAndExpiresAtAfter(
                eq(activationCode), eq(ActivationType.REGISTRATION), any(LocalDateTime.class));
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void logout_ShouldAddTokenToBlacklist_WhenTokenIsValid() {
        String accessToken = "valid.access.token";
        String refreshToken = "valid.refresh.token";
        Date expirationDate = new Date(System.currentTimeMillis() + 3600000);
        Cookie refreshCookie = new Cookie("Refresh-token", refreshToken);
        Cookie[] cookies = new Cookie[]{refreshCookie};

        when(request.getHeader("Authorization")).thenReturn("Bearer " + accessToken);
        when(request.getCookies()).thenReturn(cookies);
        when(jwtUtil.extractExpiration(accessToken)).thenReturn(expirationDate);
        when(jwtUtil.extractExpiration(refreshToken)).thenReturn(expirationDate);
        userService.logout(response, request);

        verify(jwtUtil).addToBlackList(eq(accessToken), anyLong(), eq("access"));
        verify(jwtUtil).addToBlackList(eq(refreshToken), anyLong(), eq("refresh"));
        verify(response).addCookie(any(Cookie.class));
    }

    @Test
    void loginSuccessGoogle_ShouldCreateUser_WhenEmailNotFound() {
        String googleIdToken = "google.id.token";
        Map<String, String> tokenGoogle = Map.of("idToken", googleIdToken);

        when(jwtUtil.getEmailFromGoogleToken(googleIdToken)).thenReturn(testEmail);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(any(User.class), anyInt())).thenReturn(testToken);

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.complete(false);
        when(kafkaEventService.checkCartNotEmptyRequest(anyLong())).thenReturn(future);
        Map<String, Object> result = userService.loginSuccessGoogle(tokenGoogle, response);

        assertNotNull(result);
        assertEquals(testToken, result.get("token"));
        assertEquals(false, result.get("cartHasItems"));

        verify(jwtUtil).getEmailFromGoogleToken(googleIdToken);
        verify(userRepository).findByEmail(testEmail);
        verify(userRepository).save(any(User.class));
        verify(jwtUtil, times(2)).generateToken(any(User.class), anyInt());
        verify(response).addCookie(any(Cookie.class));
    }

    @Test
    void recoveryPassword_ShouldSendCode_WhenEmailIsValid() {
        when(userRepository.findByEmailAndEnabledAndProvider(testEmail, true, ProviderType.LOCAL))
                .thenReturn(Optional.of(testUser));

        Activation activation = new Activation();
        activation.setUser(testUser);
        activation.setType(ActivationType.RESET_PASSWORD);
        when(activationRepository.save(any(Activation.class))).thenReturn(activation);

        String result = userService.recoveryPassword(testEmail);

        assertEquals("Code sent! Check your email.", result);
        verify(userRepository).findByEmailAndEnabledAndProvider(testEmail, true, ProviderType.LOCAL);
        verify(activationRepository).save(any(Activation.class));
        verify(kafkaEventService).sendPasswordResetEvent(eq(testEmail), any(Activation.class));
    }

    @Test
    void recoveryPassword_ShouldThrowException_WhenEmailNotFound() {
        when(userRepository.findByEmailAndEnabledAndProvider(testEmail, true, ProviderType.LOCAL))
                .thenReturn(Optional.empty());

        assertThrows(UserException.class, () -> userService.recoveryPassword(testEmail));
        verify(userRepository).findByEmailAndEnabledAndProvider(testEmail, true, ProviderType.LOCAL);
        verify(activationRepository, never()).save(any(Activation.class));
    }
}