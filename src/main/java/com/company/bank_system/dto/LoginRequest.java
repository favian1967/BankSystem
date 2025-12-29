package com.company.bank_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest (
        @NotBlank(message = "email is required")
        @Email(message = "please write a valid email address")
        @Size(min = 3, max = 254, message = "Email must be in range 3 - 254 symbols")
        String email,
        @NotBlank(message = "password is required!")
        @Size(min = 8, max = 128, message = "password must be in range 8 - 128 symbols")
        String password
) {

}
