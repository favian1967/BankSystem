package com.company.bank_system.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@ToString(exclude = {"user", "cards"})
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Много счетов → один юзер
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Один счёт → много карт
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<Card> cards;

    @Column(unique = true, nullable = false)
    private String accountNumber; // Генерируем автоматически

    private String accountType; // CHECKING, SAVINGS, DEPOSIT
    private String currency;    // RUB, USD, EUR

    @Column(nullable = false)
    private BigDecimal balance; // Используем BigDecimal для денег!

    @Column(nullable = false)
    private String status; // ACTIVE, BLOCKED, CLOSED

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}