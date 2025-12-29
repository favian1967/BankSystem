package com.company.bank_system.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull(message = "fromAccount is required")
        Long fromAccountId,
        @NotNull(message = "toAccount is required")
        String toAccountId,
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "sum must be bigger than 0.01")
        @DecimalMax(value = "1000000", message = "maximum for withdraw: 1,000,000")
        BigDecimal amount,
        @Size(max = 500, message = "Описание слишком длинное")
        @Pattern(regexp = "^[\\w\\s\\p{Punct}&&[^<>\"'&]]*$",
                message = "Uncorrected symbols")
        String description
) {}