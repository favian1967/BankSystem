package com.company.bank_system.controller;

import com.company.bank_system.dto.AccountResponse;
import com.company.bank_system.dto.CreateAccountRequest;
import com.company.bank_system.repo.AccountRepository;
import com.company.bank_system.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.company.bank_system.dto.UpdateAccountStatusRequest;
import com.company.bank_system.entity.enums.Account.AccountStatus;
import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Currency;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/add")
    public AccountResponse createAccount(
            @Valid @RequestBody CreateAccountRequest createAccountRequest
    ) throws Exception {
        return accountService.createAccount(createAccountRequest);
    }

    @GetMapping("/getAll")
    public List<AccountResponse> getMyAccounts(){
        return accountService.getMyAccounts();
    }

    @GetMapping("/getById/{id}")
    public AccountResponse getAccountById(
            @PathVariable("id") Long accountId
    ){
        return accountService.getAccountById(accountId);
    }

    @GetMapping("/getByNumber/{accountNumber}")
    public AccountResponse getAccountByNumber(
            @PathVariable("accountNumber") String accountNumber
    ) {
        return accountService.getAccountByAccountNumber(accountNumber);
    }

    @GetMapping("/getByType/{type}")
    public List<AccountResponse> getAccountsByType(
            @PathVariable("type") AccountType type
    ) {
        return accountService.getAccountsByType(type);
    }

    @GetMapping("/getByCurrency/{currency}")
    public List<AccountResponse> getAccountsByCurrency(
            @PathVariable("currency") Currency currency
    ) {
        return accountService.getAccountsByCurrency(currency);
    }

    @GetMapping("/getByStatus/{status}")
    public List<AccountResponse> getAccountsByStatus(
            @PathVariable("status") AccountStatus status
    ) {
        return accountService.getAccountsByStatus(status);
    }

    @GetMapping("/{id}/balance")
    public Map<String, BigDecimal> getAccountBalance(
            @PathVariable("id") Long accountId
    ) {
        BigDecimal balance = accountService.getAccountBalance(accountId);
        return Map.of("balance", balance);
    }

    @GetMapping("/totalBalance/{currency}")
    public Map<String, Object> getTotalBalanceByCurrency(
            @PathVariable("currency") Currency currency
    ) {
        BigDecimal total = accountService.getTotalBalanceByCurrency(currency);
        return Map.of("totalBalance", total, "currency", currency.name());
    }

    @PatchMapping("/{id}/status")
    public AccountResponse updateAccountStatus(
            @PathVariable("id") Long accountId,
            @Valid @RequestBody UpdateAccountStatusRequest request
    ) {
        return accountService.updateAccountStatus(accountId, request.status());
    }

    @DeleteMapping("/{id}/close")
    public Map<String, String> closeAccount(
            @PathVariable("id") Long accountId
    ) {
        accountService.closeAccount(accountId);
        return Map.of("message", "Account closed successfully", "accountId", accountId.toString());
    }

    @GetMapping("/active")
    public List<AccountResponse> getActiveAccounts() {
        return accountService.getAccountsByStatus(AccountStatus.ACTIVE);
    }

    @PatchMapping("/{id}/block")
    public AccountResponse blockAccount(
            @PathVariable("id") Long accountId
    ) {
        return accountService.updateAccountStatus(accountId, AccountStatus.BLOCKED);
    }

    @PatchMapping("/{id}/unblock")
    public AccountResponse unblockAccount(
            @PathVariable("id") Long accountId
    ) {
        return accountService.updateAccountStatus(accountId, AccountStatus.ACTIVE);
    }

    @GetMapping("/count")
    public Map<String, Long> getAccountsCount() {
        long count = accountService.getAccountsCount();
        return Map.of("count", count);
    }

    @GetMapping("/exists/{accountNumber}")
    public Map<String, Boolean> checkAccountExists(
            @PathVariable("accountNumber") String accountNumber
    ) {
        boolean exists = accountService.accountExists(accountNumber);
        return Map.of("exists", exists);
    }
}