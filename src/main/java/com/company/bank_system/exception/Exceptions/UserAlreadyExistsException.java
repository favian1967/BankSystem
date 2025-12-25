package com.company.bank_system.exception.Exceptions;

public class UserAlreadyExistsException extends BankException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }

    public UserAlreadyExistsException() {
        super("a user with such data already exists");
    }
}
