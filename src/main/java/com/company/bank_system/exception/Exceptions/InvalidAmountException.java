package com.company.bank_system.exception.Exceptions;

public class InvalidAmountException extends  BankException {
    public InvalidAmountException(String message) {
        super(message);
    }

    public InvalidAmountException() {
        super("Insufficient funds");
    }
}
