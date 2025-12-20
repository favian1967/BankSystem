package com.company.bank_system.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JWTService {

    private final SecretKey key;
    private final long expiration;

    public JWTService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expiration = expiration;
    }


    // Генерация токена
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email) // Кладём email внутрь токена
                .issuedAt(new Date()) // Дата создания
                .expiration(new Date(System.currentTimeMillis() + 86400000)) // Срок действия (24 часа)
        .signWith(key) // Подписываем секретным ключом
                .compact();   // Превращаем в строку
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(key) // Проверяем подпись
                .build()
                .parseSignedClaims(token) // Разбираем токен
                .getPayload()
                .getSubject(); // Достаём email
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
