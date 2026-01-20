package com.company.bank_system.controller;

import com.company.bank_system.dto.DepositRequest;
import com.company.bank_system.dto.TransactionResponse;
import com.company.bank_system.dto.TransferRequest;
import com.company.bank_system.dto.WithdrawRequest;
import com.company.bank_system.entity.Transaction;
import com.company.bank_system.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/account/{accountId}")
    public Page<TransactionResponse> getTransactions(
            @PathVariable Long accountId,
            @RequestParam int page,
            @RequestParam int size
    ) {
        return transactionService.getAccountTransactions(accountId, page, size);
    }


    @GetMapping("/account/{accountId}/recent")
    public List<TransactionResponse> getRecentTransactions(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "5") int limit

    ) {
        return transactionService.getRecentTransactions(accountId, limit);
    }

}
