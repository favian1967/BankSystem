package com.company.bank_system.controller;

import com.company.bank_system.dto.DepositRequest;
import com.company.bank_system.dto.TransactionResponse;
import com.company.bank_system.dto.TransferRequest;
import com.company.bank_system.dto.WithdrawRequest;
import com.company.bank_system.entity.Transaction;
import com.company.bank_system.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    public TransactionResponse deposit(
            @Valid @RequestBody DepositRequest depositRequest
    ) {
        TransactionResponse transaction = transactionService.deposit(depositRequest);
        return transaction;
    }
    @PostMapping("/withdraw")
    public TransactionResponse withdraw(
            @Valid @RequestBody WithdrawRequest withdrawRequest
    ) {
        TransactionResponse transaction = transactionService.withdraw(withdrawRequest);
        return transaction;
    }

    @PostMapping("/transfer")
    public TransactionResponse transfer(
            @Valid @RequestBody TransferRequest transferRequest
    ){
        TransactionResponse transaction = transactionService.transfer(transferRequest);
        return transaction;
    }

}
