package com.company.bank_system.dto;

import com.company.bank_system.entity.enums.Account.AccountStatus;
import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Currency;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record AccountResponse (
        Long id,
        @JsonProperty("account_number")
        String accountNumber,
        @JsonProperty("account_type")
        AccountType accountType,
        Currency currency,
        BigDecimal balance,
        AccountStatus status
){
}
