package com.company.bank_system.dto;

import com.company.bank_system.entity.enums.Currency;
import com.company.bank_system.entity.enums.Transaction.TransactionStatus;
import com.company.bank_system.entity.enums.Transaction.TransactionType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        @JsonProperty("from_account_id")
        Long fromAccountId,
        @JsonProperty("to_account_id")
        Long toAccountId,
        @JsonProperty("transaction_type")
        TransactionType transactionType,
        BigDecimal amount,
        Currency currency,
        String description,
        TransactionStatus status,
        @JsonProperty("created_at")
        LocalDateTime createdAt
) {}