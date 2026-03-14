package com.company.bank_system.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest (
        @NotBlank
        @JsonProperty("old_password")
        String oldPassword,
        @NotBlank(message = "password is required!")
        @Size(min = 8, max = 128, message = "password must be 8 - 128 symbols")
        @JsonProperty("new_password")
        String newPassword,
        @NotBlank
        @JsonProperty("repeat_new_password")
        String repeatNewPassword
) {

}