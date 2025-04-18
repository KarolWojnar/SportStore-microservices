package com.shop.authservice.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtDecoderImplTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private JwtDecoderImpl jwtDecoder;

    private final String testToken = "test.jwt.token";

    @Test
    void decode_ShouldReturnJwt_WhenTokenIsValid() {
        Claims mockClaims = mock(Claims.class);
        Date issuedAt = new Date();
        Date expiresAt = new Date(System.currentTimeMillis() + 3600000);
        String subject = "test@example.com";
        String role = "ROLE_CUSTOMER";

        when(jwtService.isBlacklisted(testToken)).thenReturn(false);
        when(jwtService.extractAllClaims(testToken)).thenReturn(mockClaims);
        when(mockClaims.getSubject()).thenReturn(subject);
        when(mockClaims.getIssuedAt()).thenReturn(issuedAt);
        when(mockClaims.getExpiration()).thenReturn(expiresAt);
        when(mockClaims.get("role")).thenReturn(role);

        Jwt jwt = jwtDecoder.decode(testToken);

        assertNotNull(jwt);
        assertEquals(testToken, jwt.getTokenValue());
        assertEquals(subject, jwt.getSubject());
        assertEquals(issuedAt.toInstant(), jwt.getIssuedAt());
        assertEquals(expiresAt.toInstant(), jwt.getExpiresAt());
        assertEquals(role, jwt.getClaim("role"));
        assertEquals("HS256", jwt.getHeaders().get("alg"));
        assertEquals("JWT", jwt.getHeaders().get("typ"));

        verify(jwtService).isBlacklisted(testToken);
        verify(jwtService).extractAllClaims(testToken);
    }

    @Test
    void decode_ShouldThrowJwtException_WhenTokenIsBlacklisted() {
        when(jwtService.isBlacklisted(testToken)).thenReturn(true);

        JwtException exception = assertThrows(JwtException.class, () -> jwtDecoder.decode(testToken));
        assertEquals("Invalid token", exception.getMessage());

        verify(jwtService).isBlacklisted(testToken);
        verify(jwtService, never()).extractAllClaims(testToken);
    }

    @Test
    void decode_ShouldThrowJwtException_WhenExtractionFails() {
        when(jwtService.isBlacklisted(testToken)).thenReturn(false);
        when(jwtService.extractAllClaims(testToken)).thenThrow(new RuntimeException("Extraction failed"));

        JwtException exception = assertThrows(JwtException.class, () -> jwtDecoder.decode(testToken));
        assertEquals("Invalid token", exception.getMessage());

        verify(jwtService).isBlacklisted(testToken);
        verify(jwtService).extractAllClaims(testToken);
    }
}