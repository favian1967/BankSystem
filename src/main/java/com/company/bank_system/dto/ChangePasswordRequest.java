package com.company.bank_system.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest (
        @NotBlank String oldPassword,
        @NotBlank(message = "password is required!")
        @Size(min = 8, max = 128, message = "password must be 8 - 128 symbols")
        String newPassword,
        @NotBlank String repeatNewPassword
) {

}