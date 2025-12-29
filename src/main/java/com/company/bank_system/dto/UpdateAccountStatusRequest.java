package com.company.bank_system.dto;

import com.company.bank_system.entity.enums.Account.AccountStatus;

public record UpdateAccountStatusRequest(
        AccountStatus status
) {
}