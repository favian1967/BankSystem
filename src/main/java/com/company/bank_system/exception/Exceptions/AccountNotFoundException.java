package com.company.bank_system.exception.Exceptions;

public class AccountNotFoundException extends BankException {

    public enum Type {
        ID, NUMBER
    }

    public AccountNotFoundException(Type type, String value) {
        super(switch (type) {
            case ID -> "Account with id " + value + " not found";
            case NUMBER -> "Account number " + value + " not found";
        });
    }
}
