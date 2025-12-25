package com.company.bank_system.exception.Exceptions;

//Low balance

import java.math.BigDecimal;

public class InsufficientFundsException extends BankException {
    public InsufficientFundsException(String message) {
        super(message);
    }
    //TODO про %d %s
    public InsufficientFundsException(Long accountId, BigDecimal required, BigDecimal available) {
        super(String.format("low balance on account %d. required: %s, available: %s",
                accountId, required, available));
    }
}
