package com.company.bank_system.dto;


public record RegisterRequest (
        String email,
        String password,
        String firstName,
        String phone
){
}
