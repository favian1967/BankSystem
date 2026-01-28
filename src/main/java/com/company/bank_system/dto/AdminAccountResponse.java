package com.company.bank_system.dto;

public record AdminAccountResponse(
        Long id,
        String accountNumber,
        Long userId
) {}