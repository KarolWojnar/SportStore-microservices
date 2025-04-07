package com.shop.productservice.configuration;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import javax.crypto.SecretKey;
import java.security.Key;

@RequiredArgsConstructor
@Slf4j
public class JwtDecoderImpl implements JwtDecoder {

    @Value("${jwt.secret}")
    private String secret;

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            log.info("Decoding token: {}", token);
            Claims claims = extractAllClaims(token);
            log.info("Claims: {}", claims);
            return Jwt.withTokenValue(token)
                    .header("alg", "HS256")
                    .header("typ", "JWT")
                    .subject(claims.getSubject())
                    .issuedAt(claims.getIssuedAt().toInstant())
                    .expiresAt(claims.getExpiration().toInstant())
                    .claim("role", claims.get("role"))
                    .build();
        } catch (Exception e) {
            throw new JwtException("Invalid token", e);
        }
    }

    public Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}