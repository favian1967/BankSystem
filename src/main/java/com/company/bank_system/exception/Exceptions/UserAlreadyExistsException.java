package com.company.bank_system.exception.Exceptions;

public class UserAlreadyExistsException extends BankException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }

}
