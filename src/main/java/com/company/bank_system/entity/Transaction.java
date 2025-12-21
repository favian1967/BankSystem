package com.company.bank_system.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "from_account_id")
    private Account fromAccount; // Откуда (nullable для пополнения)

    @ManyToOne
    @JoinColumn(name = "to_account_id")
    private Account toAccount; // Куда (nullable для снятия)

    private String transactionType; // TRANSFER, DEPOSIT, WITHDRAWAL, PAYMENT

    @Column(nullable = false)
    private BigDecimal amount;

    private String currency; // RUB, USD, EUR
    private String description;

    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, FAILED

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}