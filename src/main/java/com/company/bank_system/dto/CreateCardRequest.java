package com.company.bank_system.dto;


import com.company.bank_system.entity.enums.Cards.CardPaymentSystem;
import com.company.bank_system.entity.enums.Cards.CardType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateCardRequest (
        @NotNull(message = "accountId is required")
        @Min(0)
        @JsonProperty("account_id")
        Long accountId,
        @NotNull(message = "CardType is required")
        @JsonProperty("card_type")
        CardType cardType,
        @NotNull(message = "PaymentSystem is required")
        @JsonProperty("payment_system")
        CardPaymentSystem paymentSystem
){

}