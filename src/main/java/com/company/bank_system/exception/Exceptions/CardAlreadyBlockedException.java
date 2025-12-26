package com.company.bank_system.exception.Exceptions;

public class CardAlreadyBlockedException extends BankException {
    public CardAlreadyBlockedException(String message) {
        super(message);
    }

    public CardAlreadyBlockedException(Long cardId) {
        super("Карта " + cardId + " уже заблокирована");
    }
}