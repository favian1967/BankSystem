package com.company.bank_system.dto;

import com.company.bank_system.entity.enums.Currency;
import com.company.bank_system.entity.enums.Transaction.TransactionStatus;
import com.company.bank_system.entity.enums.Transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,                          // ✅ ID транзакции
        Long fromAccountId,               // ✅ Nullable
        Long toAccountId,                 // ✅ Nullable
        TransactionType transactionType,
        BigDecimal amount,                // ✅ Добавил
        Currency currency,
        String description,               // ✅ Добавил
        TransactionStatus status,
        LocalDateTime createdAt           // ✅ Добавил
) {}