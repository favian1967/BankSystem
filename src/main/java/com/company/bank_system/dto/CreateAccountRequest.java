package com.company.bank_system.dto;


import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Currency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


public record CreateAccountRequest(
        @NotNull(message = "Account type is required")
        AccountType accountType,
        @NotNull(message = "Currency is required")
        Currency currency
) {
}
