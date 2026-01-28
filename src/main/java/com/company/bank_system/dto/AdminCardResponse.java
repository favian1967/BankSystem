package com.company.bank_system.dto;

public record AdminCardResponse(
        Long id,
        String cardNumber,
        Long accountId,
        Long userId
) {}