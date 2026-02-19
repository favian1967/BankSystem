package com.company.bank_system.controller;

import com.company.bank_system.dto.DepositRequest;
import com.company.bank_system.dto.TransactionResponse;
import com.company.bank_system.dto.TransferRequest;
import com.company.bank_system.dto.WithdrawRequest;
import com.company.bank_system.service.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositRequest depositRequest,
            @RequestHeader("Idempotency-Key") String key
    ) {
        return ResponseEntity.ok(transactionService.deposit(depositRequest, key));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody WithdrawRequest withdrawRequest,
            @RequestHeader("Idempotency-Key") String key
    ) {
        return ResponseEntity.ok(transactionService.withdraw(withdrawRequest, key));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest transferRequest,
            @RequestHeader("Idempotency-Key") String key
    ) {
        return ResponseEntity.ok(transactionService.transfer(transferRequest, key));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @PathVariable @Positive(message = "Account ID must be positive") Long accountId,
            @RequestParam @Min(0) int page,
            @RequestParam @Min(1) @Max(100) int size
    ) {
        return ResponseEntity.ok(transactionService.getAccountTransactions(accountId, page, size));
    }

    @GetMapping("/account/{accountId}/recent")
    public ResponseEntity<List<TransactionResponse>> getRecentTransactions(
            @PathVariable @Positive(message = "Account ID must be positive") Long accountId,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int limit
    ) {
        return ResponseEntity.ok(transactionService.getRecentTransactions(accountId, limit));
    }
}
