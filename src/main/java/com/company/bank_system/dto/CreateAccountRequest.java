package com.company.bank_system.dto;


import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Currency;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


public record CreateAccountRequest(
        AccountType accountType,
        Currency currency
) {
}
