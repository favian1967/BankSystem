package com.company.bank_system.entity;


import com.company.bank_system.entity.enums.Currency;
import com.company.bank_system.entity.enums.Transaction.TransactionStatus;
import com.company.bank_system.entity.enums.Transaction.TransactionType;
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
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType; // TRANSFER, DEPOSIT, WITHDRAWAL, PAYMENT

    @Column(nullable = false)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private Currency currency; // RUB, USD, EUR
    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status; // PENDING, COMPLETED, FAILED

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;
}