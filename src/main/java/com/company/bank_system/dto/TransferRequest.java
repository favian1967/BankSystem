package com.company.bank_system.dto;

import java.math.BigDecimal;

public record TransferRequest(
        Long fromAccountId,   // Откуда
        Long toAccountId,     // Куда
        BigDecimal amount,    // Сумма
        String description
) {}