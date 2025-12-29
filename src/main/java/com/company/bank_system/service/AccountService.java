package com.company.bank_system.service;

import com.company.bank_system.dto.AccountResponse;
import com.company.bank_system.dto.CreateAccountRequest;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.Account.AccountStatus;
import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Currency;
import com.company.bank_system.exception.Exceptions.AccessDeniedException;
import com.company.bank_system.exception.Exceptions.AccountNotFoundException;
import com.company.bank_system.exception.Exceptions.InvalidOperationException;
import com.company.bank_system.repo.AccountRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    //INTERNAL USE ONLY
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
    // INTERNAL USE ONLY (payments, transfers)
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

    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        User currentUser = currentUserService.getCurrentUser();

        log.debug("GET_ACCOUNT_BY_NUMBER userId={} accountNumber={}",
                currentUser.getId(), maskAccountNumber(accountNumber)
        );

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> {
                    log.warn("ACCOUNT_NOT_FOUND_BY_NUMBER accountNumber={}",
                            maskAccountNumber(accountNumber)
                    );
                    return new AccountNotFoundException(
                            AccountNotFoundException.Type.NUMBER,
                            accountNumber
                    );
                });

        if (!account.getUser().getId().equals(currentUser.getId())) {
            log.error("ACCESS_DENIED userId={} accountNumber={}",
                    currentUser.getId(), maskAccountNumber(accountNumber)
            );
            throw new AccessDeniedException("Access denied to account");
        }

        log.info("GET_ACCOUNT_BY_NUMBER_SUCCESS userId={} accountId={}",
                currentUser.getId(), account.getId()
        );

        return mapToResponse(account);
    }

    public List<AccountResponse> getAccountsByType(AccountType type) {
        User currentUser = currentUserService.getCurrentUser();

        log.debug("GET_ACCOUNTS_BY_TYPE userId={} type={}", currentUser.getId(), type);

        List<Account> accounts = accountRepository.findByUserAndAccountType(currentUser, type);

        log.info("GET_ACCOUNTS_BY_TYPE_SUCCESS userId={} type={} count={}",
                currentUser.getId(), type, accounts.size()
        );

        return accounts.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<AccountResponse> getAccountsByCurrency(Currency currency) {
        User currentUser = currentUserService.getCurrentUser();

        log.debug("GET_ACCOUNTS_BY_CURRENCY userId={} currency={}", currentUser.getId(), currency);

        List<Account> accounts = accountRepository.findByUserAndCurrency(currentUser, currency);

        log.info("GET_ACCOUNTS_BY_CURRENCY_SUCCESS userId={} currency={} count={}",
                currentUser.getId(), currency, accounts.size()
        );

        return accounts.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<AccountResponse> getAccountsByStatus(AccountStatus status) {
        User currentUser = currentUserService.getCurrentUser();

        log.debug("GET_ACCOUNTS_BY_STATUS userId={} status={}", currentUser.getId(), status);

        List<Account> accounts = accountRepository.findByUserAndStatus(currentUser, status);

        log.info("GET_ACCOUNTS_BY_STATUS_SUCCESS userId={} status={} count={}",
                currentUser.getId(), status, accounts.size()
        );

        return accounts.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public BigDecimal getAccountBalance(Long accountId) {
        User currentUser = currentUserService.getCurrentUser();

        log.debug("GET_ACCOUNT_BALANCE userId={} accountId={}", currentUser.getId(), accountId);

        Account account = getAccountEntityById(accountId);

        log.info("GET_ACCOUNT_BALANCE_SUCCESS userId={} accountId={} balance={}",
                currentUser.getId(), accountId, account.getBalance()
        );

        return account.getBalance();
    }

    public BigDecimal getTotalBalanceByCurrency(Currency currency) {
        User currentUser = currentUserService.getCurrentUser();

        log.debug("GET_TOTAL_BALANCE userId={} currency={}", currentUser.getId(), currency);

        List<Account> accounts = accountRepository.findByUserAndCurrency(currentUser, currency);

        BigDecimal total = accounts.stream()
                .filter(acc -> acc.getStatus() == AccountStatus.ACTIVE)
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("GET_TOTAL_BALANCE_SUCCESS userId={} currency={} total={}",
                currentUser.getId(), currency, total
        );

        return total;
    }

    @Transactional
    public AccountResponse updateAccountStatus(Long accountId, AccountStatus newStatus) {
        User currentUser = currentUserService.getCurrentUser();

        log.info("UPDATE_ACCOUNT_STATUS_START userId={} accountId={} newStatus={}",
                currentUser.getId(), accountId, newStatus
        );

        Account account = getAccountEntityById(accountId);

        if (account.getStatus() == AccountStatus.CLOSED) {
            log.error("CANNOT_UPDATE_CLOSED_ACCOUNT accountId={}", accountId);
            throw new InvalidOperationException("Cannot update status of closed account");
        }

        account.setStatus(newStatus);
        account.setUpdatedAt(LocalDateTime.now());

        Account saved = accountRepository.save(account);

        log.info("UPDATE_ACCOUNT_STATUS_SUCCESS userId={} accountId={} status={}",
                currentUser.getId(), accountId, newStatus
        );

        return mapToResponse(saved);
    }

    @Transactional
    public void closeAccount(Long accountId) {
        User currentUser = currentUserService.getCurrentUser();

        log.info("CLOSE_ACCOUNT_START userId={} accountId={}", currentUser.getId(), accountId);

        Account account = getAccountEntityById(accountId);

        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            log.error("CANNOT_CLOSE_ACCOUNT_WITH_BALANCE accountId={} balance={}",
                    accountId, account.getBalance()
            );
            throw new InvalidOperationException("Cannot close account with non-zero balance");
        }

        if (account.getStatus() == AccountStatus.CLOSED) {
            log.warn("ACCOUNT_ALREADY_CLOSED accountId={}", accountId);
            throw new InvalidOperationException("Account is already closed");
        }

        account.setStatus(AccountStatus.CLOSED);
        account.setUpdatedAt(LocalDateTime.now());

        accountRepository.save(account);

        log.info("CLOSE_ACCOUNT_SUCCESS userId={} accountId={}", currentUser.getId(), accountId);
    }

    public long getAccountsCount() {
        User currentUser = currentUserService.getCurrentUser();

        log.debug("GET_ACCOUNTS_COUNT userId={}", currentUser.getId());

        long count = accountRepository.countByUser(currentUser);

        log.info("GET_ACCOUNTS_COUNT_SUCCESS userId={} count={}", currentUser.getId(), count);

        return count;
    }

    public boolean accountExists(String accountNumber) {
        log.debug("CHECK_ACCOUNT_EXISTS accountNumber={}", maskAccountNumber(accountNumber));

        boolean exists = accountRepository.existsByAccountNumber(accountNumber);

        log.info("CHECK_ACCOUNT_EXISTS_RESULT accountNumber={} exists={}",
                maskAccountNumber(accountNumber), exists
        );

        return exists;
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