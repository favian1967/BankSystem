package com.company.bank_system.dto;

import java.math.BigDecimal;

public record WithdrawRequest(
        Long accountId,       // С какого счёта снять
        BigDecimal amount,    // Сумма
        String description
) {}