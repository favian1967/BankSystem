package com.company.bank_system.service;

import com.company.bank_system.dto.AccountResponse;
import com.company.bank_system.dto.CreateAccountRequest;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.Account.AccountStatus;
import com.company.bank_system.exception.Exceptions.AccessDeniedException;
import com.company.bank_system.exception.Exceptions.AccountNotFoundException;
import com.company.bank_system.repo.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final CurrentUserService currentUserService;

    public AccountService(AccountRepository accountRepository, CurrentUserService currentUserService) {
        this.accountRepository = accountRepository;
        this.currentUserService = currentUserService;
    }

    public AccountResponse createAccount(CreateAccountRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        log.info("ACCOUNT_CREATE_START userId={} type={} currency={}",
                currentUser.getId(),
                request.accountType(),
                request.currency()
        );

        Account account = new Account();
        account.setUser(currentUser);
        account.setAccountNumber(generateAccountNumber());
        account.setAccountType(request.accountType());
        account.setCurrency(request.currency());
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());

        Account saved = accountRepository.save(account);

        log.info("ACCOUNT_CREATE_SUCCESS userId={} accountId={} accountNumber={}",
                currentUser.getId(),
                saved.getId(),
                maskAccountNumber(saved.getAccountNumber())
        );

        return mapToResponse(saved);
    }

    public List<AccountResponse> getMyAccounts() {
        User currentUser = currentUserService.getCurrentUser();

        log.debug("GET_MY_ACCOUNTS userId={}", currentUser.getId());

        List<Account> accounts = accountRepository.findByUser(currentUser);

        log.info("GET_MY_ACCOUNTS_SUCCESS userId={} count={}",
                currentUser.getId(),
                accounts.size()
        );

        return accounts.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public AccountResponse getAccountById(Long accountId) {
        User currentUser = currentUserService.getCurrentUser();

        log.debug("GET_ACCOUNT_BY_ID_START userId={} accountId={}",
                currentUser.getId(), accountId
        );

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("ACCOUNT_NOT_FOUND accountId={}", accountId);
                    return new AccountNotFoundException(
                            AccountNotFoundException.Type.ID,
                            accountId.toString()
                    );
                });

        if (!account.getUser().getId().equals(currentUser.getId())) {
            log.error("ACCESS_DENIED userId={} accountId={}",
                    currentUser.getId(), accountId
            );
            throw new AccessDeniedException("Access denied to account " + accountId);
        }

        log.info("GET_ACCOUNT_BY_ID_SUCCESS userId={} accountId={}",
                currentUser.getId(), accountId
        );

        return mapToResponse(account);
    }

    public Account getAccountEntityById(Long accountId) {
        User currentUser = currentUserService.getCurrentUser();

        log.debug("GET_ACCOUNT_ENTITY userId={} accountId={}",
                currentUser.getId(), accountId
        );

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("ACCOUNT_NOT_FOUND accountId={}", accountId);
                    return new AccountNotFoundException(
                            AccountNotFoundException.Type.ID,
                            accountId.toString()
                    );
                });

        if (!account.getUser().getId().equals(currentUser.getId())) {
            log.error("ACCESS_DENIED userId={} accountId={}",
                    currentUser.getId(), accountId
            );
            throw new AccessDeniedException("Access denied to account " + accountId);
        }

        return account;
    }

    public Account getAnyAccountById(Long accountId) {
        log.debug("GET_ANY_ACCOUNT accountId={}", accountId);

        return accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("ACCOUNT_NOT_FOUND accountId={}", accountId);
                    return new AccountNotFoundException(
                            AccountNotFoundException.Type.ID,
                            accountId.toString()
                    );
                });
    }

    public Account getAccountByNumber(String accountNumber) {
        log.debug("GET_ACCOUNT_BY_NUMBER accountNumber={}", maskAccountNumber(accountNumber));

        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.warn("ACCOUNT_NOT_FOUND_BY_NUMBER accountNumber={}",
                            maskAccountNumber(accountNumber)
                    );
                    return new AccountNotFoundException(
                            AccountNotFoundException.Type.NUMBER,
                            accountNumber
                    );
                });
    }

    private String generateAccountNumber() {
        String accountNumber;

        do {
            String prefix = "40817";
            long randomPart = ThreadLocalRandom.current()
                    .nextLong(100000000000000L, 999999999999999L);
            accountNumber = prefix + randomPart;
        } while (accountRepository.existsByAccountNumber(accountNumber));

        log.debug("ACCOUNT_NUMBER_GENERATED {}", maskAccountNumber(accountNumber));

        return accountNumber;
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

    private String maskAccountNumber(String number) {
        if (number.length() < 6) return "****";
        return number.substring(0, 4) + "****" + number.substring(number.length() - 2);
    }
}
