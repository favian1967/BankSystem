package com.company.bank_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Getter
@Setter
@ToString(exclude = {"cvvHash", "account"})
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Много карт → один счёт
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // Связь с юзером (для быстрого доступа)
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(unique = true, nullable = false)
    private String cardNumber; // 16 цифр

    private String cardHolderName; // IVAN PETROV

    @Column(nullable = false)
    private String cvvHash; // Хешированный CVV (безопасность!)

    @Column(nullable = false)
    private LocalDate expiryDate; // Срок действия

    private String cardType; // DEBIT, CREDIT

    private String paymentSystem; // VISA, MASTERCARD, MIR

    @Column(nullable = false)
    private String status; // ACTIVE, BLOCKED, EXPIRED

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}