package com.shop.authservice.service;

import com.shop.authservice.exception.UserException;
import com.shop.authservice.model.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@RequiredArgsConstructor
@Service
@Slf4j
public class JwtService {

    private final RedisTemplate<String, String> redisBlacklistTemplate;
    @Value("${jwt.secret}")
    private String secret;

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user, int exp) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + exp))
                .signWith(getSigningKey())
                .compact();
    }


    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public void addToBlackList(String token, long exp, String tokenType) {
        try {
            String key = String.format("jwt:blacklist:%s:%s", tokenType, token);
            redisBlacklistTemplate.opsForValue().set(
                    key,
                    "invalidated",
                    exp,
                    TimeUnit.MILLISECONDS
            );
            log.info("Added token to blacklist: {}", key);
        } catch (Exception e) {
            log.error("Failed to add token to Redis blacklist", e);
            throw new UserException("Failed to invalidate token.");
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            String accessKey = String.format("jwt:blacklist:access:%s", token);
            String refreshKey = String.format("jwt:blacklist:refresh:%s", token);
            return redisBlacklistTemplate.hasKey(accessKey) || redisBlacklistTemplate.hasKey(refreshKey);
        } catch (Exception e) {
            log.error("Failed to check token in Redis blacklist", e);
            return false;
        }
    }
}

