package com.company.bank_system.controller;

import com.company.bank_system.dto.AccountResponse;
import com.company.bank_system.dto.CreateAccountRequest;
import com.company.bank_system.dto.UpdateAccountStatusRequest;
import com.company.bank_system.entity.enums.Account.AccountStatus;
import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Currency;
import com.company.bank_system.service.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@Validated
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/add")
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest createAccountRequest
    ) throws Exception {
        return ResponseEntity.ok(accountService.createAccount(createAccountRequest));
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<AccountResponse>> getMyAccounts() {
        return ResponseEntity.ok(accountService.getMyAccounts());
    }

    @GetMapping("/getById/{id}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable("id") @Positive(message = "Account ID must be positive") Long accountId
    ) {
        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }

    @GetMapping("/getByNumber/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByNumber(
            @PathVariable("accountNumber") String accountNumber
    ) {
        return ResponseEntity.ok(accountService.getAccountByAccountNumber(accountNumber));
    }

    @GetMapping("/getByType/{type}")
    public ResponseEntity<List<AccountResponse>> getAccountsByType(
            @PathVariable("type") AccountType type
    ) {
        return ResponseEntity.ok(accountService.getAccountsByType(type));
    }

    @GetMapping("/getByCurrency/{currency}")
    public ResponseEntity<List<AccountResponse>> getAccountsByCurrency(
            @PathVariable("currency") Currency currency
    ) {
        return ResponseEntity.ok(accountService.getAccountsByCurrency(currency));
    }

    @GetMapping("/getByStatus/{status}")
    public ResponseEntity<List<AccountResponse>> getAccountsByStatus(
            @PathVariable("status") AccountStatus status
    ) {
        return ResponseEntity.ok(accountService.getAccountsByStatus(status));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<Map<String, BigDecimal>> getAccountBalance(
            @PathVariable("id") @Positive(message = "Account ID must be positive") Long accountId
    ) {
        BigDecimal balance = accountService.getAccountBalance(accountId);
        return ResponseEntity.ok(Map.of("balance", balance));
    }

    @GetMapping("/totalBalance/{currency}")
    public ResponseEntity<Map<String, Object>> getTotalBalanceByCurrency(
            @PathVariable("currency") Currency currency
    ) {
        BigDecimal total = accountService.getTotalBalanceByCurrency(currency);
        return ResponseEntity.ok(Map.of("totalBalance", total, "currency", currency.name()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AccountResponse> updateAccountStatus(
            @PathVariable("id") @Positive(message = "Account ID must be positive") Long accountId,
            @Valid @RequestBody UpdateAccountStatusRequest request
    ) {
        return ResponseEntity.ok(accountService.updateAccountStatus(accountId, request.status()));
    }

    @DeleteMapping("/{id}/close")
    public ResponseEntity<Void> closeAccount(
            @PathVariable("id") @Positive(message = "Account ID must be positive") Long accountId
    ) {
        accountService.closeAccount(accountId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    public ResponseEntity<List<AccountResponse>> getActiveAccounts() {
        return ResponseEntity.ok(accountService.getAccountsByStatus(AccountStatus.ACTIVE));
    }

    @PatchMapping("/{id}/block")
    public ResponseEntity<AccountResponse> blockAccount(
            @PathVariable("id") @Positive(message = "Account ID must be positive") Long accountId
    ) {
        return ResponseEntity.ok(accountService.updateAccountStatus(accountId, AccountStatus.BLOCKED));
    }

    @PatchMapping("/{id}/unblock")
    public ResponseEntity<AccountResponse> unblockAccount(
            @PathVariable("id") @Positive(message = "Account ID must be positive") Long accountId
    ) {
        return ResponseEntity.ok(accountService.updateAccountStatus(accountId, AccountStatus.ACTIVE));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getAccountsCount() {
        long count = accountService.getAccountsCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/exists/{accountNumber}")
    public ResponseEntity<Map<String, Boolean>> checkAccountExists(
            @PathVariable("accountNumber") String accountNumber
    ) {
        boolean exists = accountService.accountExists(accountNumber);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
}