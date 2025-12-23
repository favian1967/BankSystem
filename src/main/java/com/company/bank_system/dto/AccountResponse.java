package com.company.bank_system.dto;

import com.company.bank_system.entity.enums.Account.AccountStatus;
import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Currency;

import java.math.BigDecimal;

public record AccountResponse (
        Long id,
        String accountNumber,
        AccountType accountType,
        Currency currency,
        BigDecimal balance,
        AccountStatus status
){
}
