package com.company.bank_system.dto;

import java.math.BigDecimal;

public record WithdrawRequest(
        Long accountId,
        BigDecimal amount,
        String description
) {}