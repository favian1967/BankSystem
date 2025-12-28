package com.company.bank_system.dto;

import com.company.bank_system.entity.enums.Currency;
import com.company.bank_system.entity.enums.Transaction.TransactionStatus;
import com.company.bank_system.entity.enums.Transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long fromAccountId,
        Long toAccountId,
        TransactionType transactionType,
        BigDecimal amount,
        Currency currency,
        String description,
        TransactionStatus status,
        LocalDateTime createdAt
) {}