package com.company.bank_system.dto;

import java.math.BigDecimal;

public record DepositRequest(
        Long accountId,       // На какой счёт пополнить
        BigDecimal amount,    // Сумма
        String description    // Описание (опционально)
) {}