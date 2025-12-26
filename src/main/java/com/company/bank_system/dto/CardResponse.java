package com.company.bank_system.dto;

import com.company.bank_system.entity.enums.Cards.CardPaymentSystem;
import com.company.bank_system.entity.enums.Cards.CardStatus;
import com.company.bank_system.entity.enums.Cards.CardType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CardResponse (
        Long id,
        String cardNumber,
        String cardHolderName,
        LocalDate expiryDate,
        CardType cardType,
        CardPaymentSystem paymentSystem,
        CardStatus cardStatus,
        Long accountId,
        LocalDateTime createdAt
        //without cvv, its security
){
}
