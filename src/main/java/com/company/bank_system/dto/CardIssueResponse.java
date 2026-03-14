package com.company.bank_system.dto;

import com.company.bank_system.entity.enums.Cards.CardPaymentSystem;
import com.company.bank_system.entity.enums.Cards.CardStatus;
import com.company.bank_system.entity.enums.Cards.CardType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CardIssueResponse (
        Long id,
        @JsonProperty("card_number")
        String cardNumber,
        @JsonProperty("card_holder_name")
        String cardHolderName,
        @JsonProperty("expiry_date")
        LocalDate expiryDate,
        @JsonProperty("card_type")
        CardType cardType,
        @JsonProperty("payment_system")
        CardPaymentSystem paymentSystem,
        @JsonProperty("card_status")
        CardStatus cardStatus,
        @JsonProperty("account_id")
        Long accountId,
        String cvv,
        @JsonProperty("created_at")
        LocalDateTime createdAt
) {

}
