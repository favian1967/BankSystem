package com.company.bank_system.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminAccountResponse(
        Long id,
        @JsonProperty("account_number")
        String accountNumber,
        @JsonProperty("user_id")
        Long userId
) {}