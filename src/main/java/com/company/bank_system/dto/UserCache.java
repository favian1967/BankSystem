package com.company.bank_system.dto;

import com.company.bank_system.entity.enums.User.UserStatus;

public record UserCache (
        Long id,
        String email,
        String role,
        boolean confirmed,
        UserStatus status
) {

}
