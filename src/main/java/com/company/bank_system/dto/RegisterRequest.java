package com.company.bank_system.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest (
        @NotBlank(message = "email is required")
        @Email(message = "uncorrectly email")
        @Size(max = 254, message = "Email is long")
        String email,
        @NotBlank(message = "password is required!")
        //TODO in prod uncomment (cuz my account has login and password a,a ))) )
//        @Size(min = 8, max = 128, message = "password must be 8 - 128 symbols")
        String password,
        @NotBlank(message = "firstName is required")
        @Pattern(regexp = "^[A-Za-zА-Яа-яЁё\\-\\s]+$", message = "Only symbols:  A-Za-zА-Яа-яЁё")
        String firstName,
        @NotBlank(message = "phone is required")
        @Pattern(regexp = "^\\+7\\d{10}$",
                message = "phone must be in format: +7XXXXXXXXXX")
        String phone
){
}
