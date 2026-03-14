package com.company.bank_system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminCreateAccountRequest(
        @JsonProperty("user_id")
        Long userId,
        @JsonProperty("account_number")
        String accountNumber
) {}