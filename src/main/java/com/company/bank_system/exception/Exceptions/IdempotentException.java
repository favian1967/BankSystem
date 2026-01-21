package com.company.bank_system.exception.Exceptions;

public class IdempotentException extends BankException {
    public IdempotentException(String message) {
        super(message);
    }
}
