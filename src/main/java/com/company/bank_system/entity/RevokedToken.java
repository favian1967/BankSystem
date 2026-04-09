//package com.company.bank_system.entity;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.Setter;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Getter
//@Setter
//@Table(name = "revoked_tokens")
//public class RevokedToken {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false, unique = true, length = 500)
//    private String token;
//
//    @Column(nullable = false)
//    private LocalDateTime revokedAt;
//
//    public RevokedToken() {}
//
//    public RevokedToken(String token) {
//        this.token = token;
//        this.revokedAt = LocalDateTime.now();
//    }
//
//}