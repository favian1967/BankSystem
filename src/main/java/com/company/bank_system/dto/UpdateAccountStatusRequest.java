package com.company.bank_system.dto;

import com.company.bank_system.entity.enums.Account.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountStatusRequest(
        @NotNull(message = "status is required")
        AccountStatus status
) {
}