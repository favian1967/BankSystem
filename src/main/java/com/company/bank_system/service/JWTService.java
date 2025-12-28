package com.company.bank_system.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;


@Service
@Slf4j
public class JWTService {

    private final SecretKey key;
    private final long expiration;

    public JWTService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiration = expiration;
        log.info("JWT_SERVICE_INITIALIZED expiration={}ms", expiration);
    }

    public String generateToken(String email) {
        log.debug("JWT_GENERATE_START email={}", email);

        String token = Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();

        log.info("JWT_GENERATE_SUCCESS email={}", email);

        return token;
    }

    public String extractEmail(String token) {
        log.debug("JWT_EXTRACT_EMAIL_START");

        String email = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        log.debug("JWT_EXTRACT_EMAIL_SUCCESS email={}", email);

        return email;
    }

    public boolean isValid(String token) {
        log.debug("JWT_VALIDATE_START");

        try {
            Jwts.parser().verifyWith(key).build().parseClaimsJws(token);
            log.debug("JWT_VALIDATE_SUCCESS");
            return true;
        } catch (Exception e) {
            log.warn("JWT_VALIDATE_FAILED reason={}", e.getMessage());
            return false;
        }
    }
}