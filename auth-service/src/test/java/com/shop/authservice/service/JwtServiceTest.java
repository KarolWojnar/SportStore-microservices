package com.shop.authservice.service;

import com.shop.authservice.exception.UserException;
import com.shop.authservice.model.Roles;
import com.shop.authservice.model.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private RedisTemplate<String, String> redisBlacklistTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private JwtService jwtService;

    private User testUser;

    @BeforeEach
    void setUp() {
        String testSecret = "dGhpc2lzYXRlc3RzZWNyZXRrZXlmb3JqdXN0dGVzdGluZ3B1cnBvc2Vzb25seQ==";
        ReflectionTestUtils.setField(jwtService, "secret", testSecret);
        String googleClientId = "google-client-id.apps.googleusercontent.com";
        ReflectionTestUtils.setField(jwtService, "googleClientId", googleClientId);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setRole(Roles.ROLE_CUSTOMER);
    }

    @Test
    void generateToken_ShouldCreateValidToken() {
        int expirationTime = 3600000;
        String token = jwtService.generateToken(testUser, expirationTime);

        assertNotNull(token);
        Claims claims = jwtService.extractAllClaims(token);
        assertEquals(testUser.getEmail(), claims.getSubject());
        assertEquals(testUser.getRole().name(), claims.get("role"));
        assertNotNull(claims.getExpiration());

        long expectedExpTime = System.currentTimeMillis() + expirationTime;
        long actualExpTime = claims.getExpiration().getTime();
        assertTrue(Math.abs(expectedExpTime - actualExpTime) < 10000);
    }

    @Test
    void extractAllClaims_ShouldReturnCorrectClaims() {
        String token = jwtService.generateToken(testUser, 3600000);
        Claims claims = jwtService.extractAllClaims(token);

        assertNotNull(claims);
        assertEquals(testUser.getEmail(), claims.getSubject());
        assertEquals(testUser.getRole().name(), claims.get("role"));
    }

    @Test
    void extractExpiration_ShouldReturnCorrectDate() {
        int expirationTime = 3600000;
        String token = jwtService.generateToken(testUser, expirationTime);
        Date expiration = jwtService.extractExpiration(token);

        assertNotNull(expiration);
        long expectedExpTime = System.currentTimeMillis() + expirationTime;
        long actualExpTime = expiration.getTime();
        assertTrue(Math.abs(expectedExpTime - actualExpTime) < 10000);
    }

    @Test
    void addToBlackList_ShouldAddTokenToRedis() {
        when(redisBlacklistTemplate.opsForValue()).thenReturn(valueOperations);
        String token = "test-token";
        long expTime = 3600000;
        String tokenType = "access";
        jwtService.addToBlackList(token, expTime, tokenType);

        verify(valueOperations).set(
                eq("jwt:blacklist:access:test-token"),
                eq("invalidated"),
                eq(expTime),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    void addToBlackList_ShouldThrowException_WhenRedisFails() {
        when(redisBlacklistTemplate.opsForValue()).thenReturn(valueOperations);
        String token = "test-token";
        long expTime = 3600000;
        String tokenType = "access";

        doThrow(new RuntimeException("Redis error")).when(valueOperations)
                .set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        assertThrows(UserException.class, () -> jwtService.addToBlackList(token, expTime, tokenType));
    }

    @Test
    void isBlacklisted_ShouldReturnTrue_WhenTokenInAccessBlacklist() {
        String token = "test-token";
        when(redisBlacklistTemplate.hasKey("jwt:blacklist:access:test-token")).thenReturn(true);
        boolean result = jwtService.isBlacklisted(token);

        assertTrue(result);
    }

    @Test
    void isBlacklisted_ShouldReturnFalse_WhenTokenNotBlacklisted() {
        String token = "test-token";
        when(redisBlacklistTemplate.hasKey("jwt:blacklist:access:test-token")).thenReturn(false);
        when(redisBlacklistTemplate.hasKey("jwt:blacklist:refresh:test-token")).thenReturn(false);

        boolean result = jwtService.isBlacklisted(token);
        assertFalse(result);
    }

    @Test
    void isBlacklisted_ShouldReturnFalse_WhenRedisThrowsException() {
        String token = "test-token";
        when(redisBlacklistTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis error"));
        boolean result = jwtService.isBlacklisted(token);

        assertFalse(result);
        verify(redisBlacklistTemplate).hasKey("jwt:blacklist:access:test-token");
    }
}