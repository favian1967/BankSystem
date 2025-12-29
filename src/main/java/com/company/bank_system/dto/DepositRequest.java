package com.company.bank_system.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DepositRequest(
        @NotNull(message = "accountId is required")
        @Min(0)
        Long accountId,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "sum must be bigger than 0.01")
        @DecimalMax(value = "1000000", message = "maximum for withdraw: 1,000,000")
        BigDecimal amount,

        @Pattern(regexp = "^[\\w\\s\\p{Punct}&&[^<>\"'&]]*$",
                message = "Uncorrected symbols")
        String description
) {}