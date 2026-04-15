package com.company.bank_system.service;

import com.company.bank_system.dto.AccountResponse;
import com.company.bank_system.dto.CreateAccountRequest;
import com.company.bank_system.dto.UserCache;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.Account.AccountStatus;
import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Currency;
import com.company.bank_system.exception.Exceptions.*;
import com.company.bank_system.repo.AccountRepository;
import com.company.bank_system.repo.UserRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
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
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository, CurrentUserService currentUserService, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#root.target.getCurrentUserCacheKey()")
    public AccountResponse createAccount(CreateAccountRequest request)  {
        UserCache currentUser = currentUserService.getCurrentUser();

        if (!currentUser.confirmed()) {
            throw new UserNotConfirmedException("User is not confirmed");
        }

        User userEntity = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new UserNotFoundException("User not found"));


        log.info("ACCOUNT_CREATE_START userId={} type={} currency={}",
                currentUser.id(),
                request.accountType(),
                request.currency()
        );

        Account account = new Account();
        account.setUser(userEntity);
        account.setAccountNumber(generateAccountNumber());
        account.setAccountType(request.accountType());
        account.setCurrency(request.currency());
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());

        Account saved = accountRepository.save(account);

        log.info("ACCOUNT_CREATE_SUCCESS userId={} accountId={} accountNumber={}",
                currentUser.id(),
                saved.getId(),
                maskAccountNumber(saved.getAccountNumber())
        );

        return mapToResponse(saved);
    }

    @Cacheable(value = "accounts", key = "#root.target.getCurrentUserCacheKey()")
    public List<AccountResponse> getMyAccounts() {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.debug("GET_MY_ACCOUNTS userId={}", currentUser.id());

        List<Account> accounts = accountRepository.findByUserId(currentUser.id());

        log.info("GET_MY_ACCOUNTS_SUCCESS userId={} count={}",
                currentUser.id(),
                accounts.size()
        );

        return accounts.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public AccountResponse getAccountById(Long accountId) {
        UserCache currentUser = currentUserService.getCurrentUser();

        Account account = findWithOwnershipCheck(accountId, currentUser.id());
        log.info("GET_ACCOUNT_BY_ID_SUCCESS userId={} accountId={}",
                currentUser.id(), accountId
        );

        return mapToResponse(account);
    }

    public Account getAccountEntityById(Long accountId) {
        Long userId = currentUserService.getCurrentUser().id();
        return findWithOwnershipCheck(accountId, userId);
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

    @Transactional
    public Account getAnyAccountByIdForUpdate(Long accountId) {
        log.debug("GET_ANY_ACCOUNT_FOR_UPDATE accountId={}", accountId);

        return accountRepository.findByIdForUpdate(accountId)
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

    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        UserCache currentUser = currentUserService.getCurrentUser();

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

        if (!account.getUser().getId().equals(currentUser.id())) {
            log.error("ACCESS_DENIED userId={} accountNumber={}",
                    currentUser.id(), maskAccountNumber(accountNumber)
            );
            throw new AccessDeniedException("Access denied to account");
        }

        log.info("GET_ACCOUNT_BY_NUMBER_SUCCESS userId={} accountId={}",
                currentUser.id(), account.getId()
        );

        return mapToResponse(account);
    }

    public List<AccountResponse> getAccountsByType(AccountType type) {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.debug("GET_ACCOUNTS_BY_TYPE userId={} type={}", currentUser.id(), type);

        List<Account> accounts = accountRepository.findByUserIdAndAccountType(currentUser.id(), type);

        log.info("GET_ACCOUNTS_BY_TYPE_SUCCESS userId={} type={} count={}",
                currentUser.id(), type, accounts.size()
        );

        return accounts.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<AccountResponse> getAccountsByCurrency(Currency currency) {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.debug("GET_ACCOUNTS_BY_CURRENCY userId={} currency={}", currentUser.id(), currency);

        List<Account> accounts = accountRepository.findByUserIdAndCurrency(currentUser.id(), currency);

        log.info("GET_ACCOUNTS_BY_CURRENCY_SUCCESS userId={} currency={} count={}",
                currentUser.id(), currency, accounts.size()
        );

        return accounts.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<AccountResponse> getAccountsByStatus(AccountStatus status) {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.debug("GET_ACCOUNTS_BY_STATUS userId={} status={}", currentUser.id(), status);

        List<Account> accounts = accountRepository.findByUserIdAndStatus(currentUser.id(), status);

        log.info("GET_ACCOUNTS_BY_STATUS_SUCCESS userId={} status={} count={}",
                currentUser.id(), status, accounts.size()
        );

        return accounts.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public BigDecimal getAccountBalance(Long accountId) {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.debug("GET_ACCOUNT_BALANCE userId={} accountId={}", currentUser.id(), accountId);

        Account account = getAccountEntityById(accountId);

        log.info("GET_ACCOUNT_BALANCE_SUCCESS userId={} accountId={} balance={}",
                currentUser.id(), accountId, account.getBalance()
        );

        return account.getBalance();
    }

    public BigDecimal getTotalBalanceByCurrency(Currency currency) {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.debug("GET_TOTAL_BALANCE userId={} currency={}", currentUser.id(), currency);

        List<Account> accounts = accountRepository.findByUserIdAndCurrency(currentUser.id(), currency);

        BigDecimal total = accounts.stream()
                .filter(acc -> acc.getStatus() == AccountStatus.ACTIVE)
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("GET_TOTAL_BALANCE_SUCCESS userId={} currency={} total={}",
                currentUser.id(), currency, total
        );

        return total;
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#root.target.getCurrentUserCacheKey()")
    public AccountResponse updateAccountStatus(Long accountId, AccountStatus newStatus) {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.info("UPDATE_ACCOUNT_STATUS_START userId={} accountId={} newStatus={}",
                currentUser.id(), accountId, newStatus
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
                currentUser.id(), accountId, newStatus
        );

        return mapToResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#root.target.getCurrentUserCacheKey()")
    public void closeAccount(Long accountId) {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.info("CLOSE_ACCOUNT_START userId={} accountId={}", currentUser.id(), accountId);

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

        log.info("CLOSE_ACCOUNT_SUCCESS userId={} accountId={}", currentUser.id(), accountId);
    }

    public long getAccountsCount() {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.debug("GET_ACCOUNTS_COUNT userId={}", currentUser.id());

        long count = accountRepository.countByUserId(currentUser.id());

        log.info("GET_ACCOUNTS_COUNT_SUCCESS userId={} count={}", currentUser.id(), count);

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

    private Account findWithOwnershipCheck(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("ACCOUNT_NOT_FOUND accountId={}", accountId);
                    return new AccountNotFoundException(
                            AccountNotFoundException.Type.ID,
                            accountId.toString()
                    );
                });

        if (!account.getUser().getId().equals(userId)) {
            log.error("ACCESS_DENIED userId={} accountId={}",
                    userId, accountId);
            throw new AccessDeniedException("Access denied to account " + accountId);
        }

        return account;
    }

    private String generateAccountNumber() {
        String accountNumber;

        do {
            String prefix = "99999999";
            long randomPart = ThreadLocalRandom.current()
                    .nextLong(10000000L, 99999999L);
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

    //only for cache #root.target.getCurrentUserCacheKey()
    @SuppressWarnings("unused")
    public String getCurrentUserCacheKey() {
        return currentUserService.getCurrentEmail();
    }

}