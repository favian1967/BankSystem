package com.company.bank_system.entity;


import com.company.bank_system.entity.enums.Account.AccountStatus;
import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Currency;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL)
    private List<Card> cards;

    @Column(unique = true, nullable = false)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    private AccountType accountType; // CHECKING, SAVINGS, DEPOSIT
    @Enumerated(EnumType.STRING)
    private Currency currency;    // RUB, USD, EUR

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus status; // ACTIVE, BLOCKED, CLOSED

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static Account create(
            User user,
            AccountType accountType,
            Currency currency,
            String accountNumber
    ){

        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (accountType == null) throw new IllegalArgumentException("AccountType cannot be null");
        if (currency == null) throw new IllegalArgumentException("Currency cannot be null");
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is invalid");
        }

        Account account = new Account();
        account.setUser(user);
        account.setAccountNumber(accountNumber);
        account.setAccountType(accountType);
        account.setCurrency(currency);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);

        return account;
    }

    public void changeStatus(AccountStatus newStatus) {
        if (this.status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot update closed account");
        }
        if (this.status == newStatus) {
            throw new IllegalStateException("Account is already has this status");
        }
        this.status = newStatus;
    }

    public void close(){
        if (this.status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Account is already closed");
        }
        if (this.balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Cannot close account with non-zero balance");
        }
        this.status = AccountStatus.CLOSED;
    }

    public boolean isActive () {
        return this.status == AccountStatus.ACTIVE;
    }


}