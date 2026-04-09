package com.company.bank_system.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenRevocationService {

    private final RedisTemplate<String, String> redisTemplate;

    public TokenRevocationService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void revoke(String token) {
//        revokedTokenRepository.save(new RevokedToken(token));
        redisTemplate.opsForValue().set(
                token,
                "revoked",
                Duration.ofHours(1) // TTL = срок жизни JWT
        );
    }

    public boolean isRevoked(String token) {
//        return revokedTokenRepository.existsByToken(token);
        return Boolean.TRUE.equals(redisTemplate.hasKey(token));
    }
}