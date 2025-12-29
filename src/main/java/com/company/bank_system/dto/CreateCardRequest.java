package com.company.bank_system.dto;


import com.company.bank_system.entity.enums.Cards.CardPaymentSystem;
import com.company.bank_system.entity.enums.Cards.CardType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateCardRequest (
        @NotNull(message = "accountId is required")
        @Min(0)
        Long accountId,
        @NotNull(message = "CardType is required")
        CardType cardType,
        @NotNull(message = "PaymentSystem is required")
        CardPaymentSystem paymentSystem
){

}
