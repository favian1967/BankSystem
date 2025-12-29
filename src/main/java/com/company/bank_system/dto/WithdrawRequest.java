package com.company.bank_system.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record WithdrawRequest(
        @NotNull(message = "id is required")
        @Positive(message = "id must be positive")
        Long accountId,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "sum must be bigger than 0.01")
        @DecimalMax(value = "1000000", message = "maximum for withdraw: 1,000,000")
        BigDecimal amount,
        @Pattern(regexp = "^[\\w\\s\\p{Punct}&&[^<>\"'&]]*$",
                message = "Uncorrected symbols")
        String description
) {}