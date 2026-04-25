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
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_from", columnList = "from_account_id"),
        @Index(name = "idx_tx_to", columnList = "to_account_id"),
        @Index(name = "idx_tx_created", columnList = "createdAt")
})
@Getter
@Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private Account fromAccount; //  (nullable for deposit)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount; // (nullable withdraw)
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

    public static Transaction buildTransaction(
            Account from,
            Account to,
            TransactionType type,
            BigDecimal amount,
            String description,
            Currency currency
    ) {
        Transaction tx = new Transaction();
        tx.setFromAccount(from);
        tx.setToAccount(to);
        tx.setTransactionType(type);
        tx.setAmount(amount);
        tx.setCurrency(currency);
        tx.setDescription(description);
        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setCompletedAt(LocalDateTime.now());
        return tx;
    }


}