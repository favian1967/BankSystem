package com.company.bank_system.dto;

public record AdminCreateCardRequest(
        Long accountId,
        Long userId,
        String cardNumber
) {}