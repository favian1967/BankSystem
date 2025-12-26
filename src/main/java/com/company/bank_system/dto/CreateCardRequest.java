package com.company.bank_system.dto;


import com.company.bank_system.entity.enums.Cards.CardPaymentSystem;
import com.company.bank_system.entity.enums.Cards.CardType;

public record CreateCardRequest (
        Long accountId,
        CardType cardType,
        CardPaymentSystem paymentSystem
){

}
