package com.company.bank_system.controller;

import com.company.bank_system.dto.AccountResponse;
import com.company.bank_system.dto.CreateAccountRequest;
import com.company.bank_system.entity.Account;
import com.company.bank_system.repo.AccountRepository;
import com.company.bank_system.service.AccountService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;
    private final AccountRepository accountRepository;

    public AccountController(AccountService accountService, AccountRepository accountRepository) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
    }

    @PostMapping("/add")
    public AccountResponse createAccount(
            @RequestBody CreateAccountRequest createAccountRequest
    ) {
        return accountService.createAccount(createAccountRequest);
    }

    @GetMapping("/getAll")
    public List<Account> getMyAccounts(){
        return accountService.getMyAccounts();
    }

    @GetMapping("/getById/{id}")
    public Account getAccountById(
            @PathVariable("id") String accountId
            ){
        return accountService.getAccountById(accountId);
    }






}
