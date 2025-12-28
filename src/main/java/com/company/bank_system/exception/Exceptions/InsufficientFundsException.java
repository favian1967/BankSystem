package com.company.bank_system.exception.Exceptions;

import java.math.BigDecimal;

public class InsufficientFundsException extends BankException {
    public InsufficientFundsException(String message) {
        super(message);
    }
    public InsufficientFundsException(Long accountId, BigDecimal required, BigDecimal available) {
        super(String.format("low balance on account %d. required: %s, available: %s",
                accountId, required, available));
    }
}
