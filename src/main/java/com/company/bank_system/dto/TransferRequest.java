package com.company.bank_system.dto;

import java.math.BigDecimal;

public record TransferRequest(
        Long fromAccountId,
        String toAccountId,
        BigDecimal amount,
        String description
) {}