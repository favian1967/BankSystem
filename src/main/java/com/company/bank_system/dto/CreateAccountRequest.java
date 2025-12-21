package com.company.bank_system.dto;


import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateAccountRequest {
    private Long userId;
    private String accountType;
    private String currency;
    private BigDecimal balance;

    private String status;
}
