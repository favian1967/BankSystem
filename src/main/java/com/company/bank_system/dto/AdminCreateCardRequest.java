package com.company.bank_system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminCreateCardRequest(
        @JsonProperty("account_id")
        Long accountId,
        @JsonProperty("user_id")
        Long userId,
        @JsonProperty("card_number")
        String cardNumber
) {}