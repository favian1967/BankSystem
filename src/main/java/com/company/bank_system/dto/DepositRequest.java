package com.company.bank_system.dto;

import java.math.BigDecimal;

public record DepositRequest(
        Long accountId,
        BigDecimal amount,
        String description
) {}