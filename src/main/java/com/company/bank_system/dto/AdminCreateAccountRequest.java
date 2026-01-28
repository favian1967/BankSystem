package com.company.bank_system.dto;

public record AdminCreateAccountRequest(
        Long userId,
        String accountNumber
) {}