package com.company.bank_system.exception.Exceptions;

public class AccessDeniedException extends BankException {
    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException() {
        super("access denied");
    }

}