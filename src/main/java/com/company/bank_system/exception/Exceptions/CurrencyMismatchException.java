package com.company.bank_system.exception.Exceptions;

public class CurrencyMismatchException extends BankException {
    public CurrencyMismatchException(String message) {
        super(message);
    }

    public CurrencyMismatchException() {
        super("Currency conflicted");
    }

}
