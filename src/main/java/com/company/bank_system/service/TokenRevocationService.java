package com.company.bank_system.service;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;

@Service
@Slf4j
public class TokenRevocationService {

    private static final String REVOKED_PREFIX = "revoked:";

    private final RedisTemplate<String, String> redisTemplate;
    private final JWTService jwtService;

    public TokenRevocationService(RedisTemplate<String, String> redisTemplate, JWTService jwtService) {
        this.redisTemplate = redisTemplate;
        this.jwtService = jwtService;
    }

    public void revoke(String token) {
        long ttlMs = remainingTtlMs(token);
        if (ttlMs <= 0) {
            log.debug("TOKEN_REVOKE_SKIPPED reason=already_expired");
            return;
        }

        redisTemplate.opsForValue().set(
                redisKey(token),
                "1",
                Duration.ofMillis(ttlMs)
        );
        log.info("TOKEN_REVOKED ttlMs={}", ttlMs);
    }

    public boolean isRevoked(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey(token)));
    }

    private long remainingTtlMs(String token) {
        try {
            Date exp = jwtService.extractExpiration(token);
            return exp.getTime() - System.currentTimeMillis();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("TOKEN_REVOKE_FAILED_PARSE reason={}", e.getClass().getSimpleName());
            return 0L;
        }
    }

    private String redisKey(String token) {
        return REVOKED_PREFIX + sha256Hex(token);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
