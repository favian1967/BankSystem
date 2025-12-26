package com.company.bank_system.exception.Exceptions;


public class CardNotFoundException extends BankException {
    public CardNotFoundException(String message) {
        super(message);
    }

    public CardNotFoundException(Long cardId) {
        super("Карта с ID " + cardId + " не найдена");
    }

}
