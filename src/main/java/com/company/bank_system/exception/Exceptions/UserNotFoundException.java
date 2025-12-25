package com.company.bank_system.exception.Exceptions;

public class UserNotFoundException extends  BankException {
    public UserNotFoundException(String message) {
        super(message);
    }


    public UserNotFoundException() {
        super("User not found");
    }

}
