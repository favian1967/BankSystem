package com.company.bank_system.service;

import com.company.bank_system.dto.AccountResponse;
import com.company.bank_system.dto.CreateAccountRequest;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.Account.AccountStatus;
import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Currency;
import com.company.bank_system.repo.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CurrentUserService currentUserService;


    public AccountService(AccountRepository accountRepository, CurrentUserService currentUserService) {
        this.accountRepository = accountRepository;
        this.currentUserService = currentUserService;
    }

    public AccountResponse createAccount(CreateAccountRequest createAccountRequest) {
        User currentUser = currentUserService.getCurrentUser();

        Account account = new Account();
        account.setUser(currentUser);
        account.setAccountNumber(generateAccountNumber());
        account.setAccountType(createAccountRequest.accountType());
        account.setCurrency(createAccountRequest.currency());
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());

        Account createdAccount = accountRepository.save(account);

        return mapToResponse(createdAccount);
    }


    public List<AccountResponse> getMyAccounts(){
        User currentUser = currentUserService.getCurrentUser();
        List<Account> account = accountRepository.findByUser(currentUser);

        return account.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public AccountResponse getAccountById(Long accountId){
        User currentUser = currentUserService.getCurrentUser();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Счёт не найден"));

        if (!account.getUser().getId().equals(currentUser.getId())){
            throw new RuntimeException("no your account");
        }

        return mapToResponse(account);
    }



    private String generateAccountNumber() {
        String accountNumber;

        do {
            String prefix = "40817";
            long randomPart = ThreadLocalRandom.current()
                    .nextLong(100000000000000L, 999999999999999L);
            accountNumber = prefix + randomPart;
        } while (accountRepository.existsByAccountNumber(accountNumber));

        return accountNumber;
    }



    public Account getAccountEntityById(Long accountId) {
        User currentUser = currentUserService.getCurrentUser();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Счёт не найден"));

        if (!account.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Доступ запрещён");
        }

        return account;
    }

    private AccountResponse mapToResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCurrency(),
                account.getBalance(),
                account.getStatus()
        );
    }


}
