package com.company.bank_system.exception.Exceptions;

import com.company.bank_system.entity.enums.Account.AccountStatus;
import lombok.Getter;

@Getter
public class AccountOperationException extends RuntimeException {

    private final Long accountId;
    private final AccountStatus status;

    public AccountOperationException(Long accountId, AccountStatus status) {
        super(buildMessage(accountId, status));
        this.accountId = accountId;
        this.status = status;
    }

    private static String buildMessage(Long accountId, AccountStatus status) {
        return switch (status) {
            case BLOCKED -> "Account " + accountId + " is blocked and cannot perform transactions";
            case CLOSED  -> "Account " + accountId + " is closed and cannot perform transactions";
            default      -> "Account " + accountId + " has invalid status: " + status;
        };
    }
}
