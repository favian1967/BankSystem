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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

@Service
@Slf4j
public class AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private static final int ATTEMPTS_FOR_GENERATION_ACC_NUMB = 5;

    public AccountService(AccountRepository accountRepository, CurrentUserService currentUserService, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    // 1

    @Transactional
    @CacheEvict(value = "accounts", key = "#root.target.getCurrentUserCacheKey()")
    public AccountResponse createAccount(CreateAccountRequest request)  {
        UserCache currentUser = getValidatedCurrentUser();
        User userEntity = getUserEntityById(currentUser.id());

        logAccountCreateStart(currentUser, request);

        Account account = Account.create(
                userEntity,
                request.accountType(),
                request.currency(),
                generateAccountNumber()
        );
        Account saved = saveWithUniqueNumber(account);

        logAccountCreateSuccess(currentUser, saved);

        return mapToResponse(saved);
    }

    @Cacheable(value = "accounts", key = "#root.target.getCurrentUserCacheKey()")
    public List<AccountResponse> getMyAccounts() {
        UserCache currentUser = getValidatedCurrentUser();

        log.debug("GET_MY_ACCOUNTS userId={}", currentUser.id());

        List<Account> accounts = accountRepository.findByUserId(currentUser.id());

        return accounts.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public AccountResponse getAccountById(Long accountId) {
        UserCache currentUser = getValidatedCurrentUser();

        Account account = getUserAccountByIdOrThrow(accountId, currentUser.id());
        log.info("GET_ACCOUNT_BY_ID_SUCCESS userId={} accountId={}",
                currentUser.id(), accountId
        );

        return mapToResponse(account);
    }

    public Account getAccountEntityById(Long accountId) {
        Long userId = getValidatedCurrentUser().id();
        return getUserAccountByIdOrThrow(accountId, userId);
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

    @Transactional
    public Account getMyAccountByIdForUpdate(Long accountId) {
        Long userId = getValidatedCurrentUser().id();
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> {
                    log.warn("ACCOUNT_NOT_FOUND accountId={}", accountId);
                    return new AccountNotFoundException(
                            AccountNotFoundException.Type.ID,
                            accountId.toString()
                    );
                });
        return checkOwnership(account, userId);
    }

    public void verifyOwnership(Account account) {
        Long userId = getValidatedCurrentUser().id();
        checkOwnership(account, userId);
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
        UserCache currentUser = getValidatedCurrentUser();

        Account account = getUserAccountByNumberOrThrow(
                accountNumber,
                currentUser.id()
        );

        log.info("GET_ACCOUNT_BY_NUMBER_SUCCESS userId={} accountId={}",
                currentUser.id(), account.getId()
        );

        return mapToResponse(account);
    }

    public List<AccountResponse> getAccountsByType(AccountType type) {
        UserCache currentUser = getValidatedCurrentUser();

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
        UserCache currentUser = getValidatedCurrentUser();

        List<Account> accounts = accountRepository.findByUserIdAndCurrency(currentUser.id(), currency);

        log.info("GET_ACCOUNTS_BY_CURRENCY_SUCCESS userId={} currency={} count={}",
                currentUser.id(), currency, accounts.size()
        );

        return accounts.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<AccountResponse> getAccountsByStatus(AccountStatus status) {
        UserCache currentUser = getValidatedCurrentUser();

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
        UserCache currentUser = getValidatedCurrentUser();
        Account account = getAccountEntityById(accountId);

        log.info("GET_ACCOUNT_BALANCE_SUCCESS userId={} accountId={} balance={}",
                currentUser.id(), accountId, account.getBalance()
        );

        return account.getBalance();
    }

    public BigDecimal getTotalBalanceByCurrency(Currency currency) {
        UserCache currentUser = getValidatedCurrentUser();

        log.debug("GET_TOTAL_BALANCE userId={} currency={}", currentUser.id(), currency);

        List<Account> accounts = accountRepository.findByUserIdAndCurrency(currentUser.id(), currency);

        BigDecimal total = accounts.stream()
                .filter(Account::isActive)
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
        UserCache currentUser = getValidatedCurrentUser();
        Account account = getAccountEntityById(accountId);

        account.changeStatus(newStatus);

        Account saved = accountRepository.save(account);

        log.info("UPDATE_ACCOUNT_STATUS_SUCCESS userId={} accountId={} status={}",
                currentUser.id(), accountId, newStatus
        );

        return mapToResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#root.target.getCurrentUserCacheKey()")
    public void closeAccount(Long accountId) {
        UserCache currentUser = getValidatedCurrentUser();
        Account account = getAccountEntityById(accountId);

        account.close();

        accountRepository.save(account);

        log.info("CLOSE_ACCOUNT_SUCCESS userId={} accountId={}", currentUser.id(), accountId);
    }

    public long getAccountsCount() {
        UserCache currentUser = getValidatedCurrentUser();

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

    // 2

    private Account getUserAccountByIdOrThrow(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("ACCOUNT_NOT_FOUND accountId={}", accountId);
                    return new AccountNotFoundException(
                            AccountNotFoundException.Type.ID,
                            accountId.toString()
                    );
                });

        return checkOwnership(account, userId);
    }

    private Account getUserAccountByNumberOrThrow(String accountNumber, Long userId) {
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

        return checkOwnership(account, userId);
    }

    private Account checkOwnership(Account account, Long userId) {
        if (!account.getUser().getId().equals(userId)) {
            log.error("ACCESS_DENIED userId={} accountNumber={}",
                    userId, maskAccountNumber(account.getAccountNumber())
            );
            throw new AccessDeniedException("Access denied to account");
        }
        return account;
    }

    private UserCache getValidatedCurrentUser(){
        UserCache currentUser = currentUserService.getCurrentUser();

        if (!currentUser.confirmed()) {
            throw new UserNotConfirmedException("User is not confirmed");
        }

        return currentUser;
    }

    private User getUserEntityById(Long userId){
        return userRepository.findUserById(userId)
                .orElseThrow(
                        () -> new UserNotFoundException("User not found id: " + userId)
        );
    }

    private void logAccountCreateStart(UserCache user, CreateAccountRequest request) {
        log.info("ACCOUNT_CREATE_START userId={} type={} currency={}",
                user.id(),
                request.accountType(),
                request.currency());
    }

    private void logAccountCreateSuccess(UserCache user, Account account) {
        log.info("ACCOUNT_CREATE_SUCCESS userId={} accountId={} accountNumber={}",
                user.id(),
                account.getId(),
                maskAccountNumber(account.getAccountNumber()));
    }

    //3


    private String generateAccountNumber() {
        String prefix = "99999999";
        long randomPart = RANDOM.nextLong(10_000_000L, 100_000_000L);

        String accountNumber = prefix + randomPart;

        log.debug("ACCOUNT_NUMBER_GENERATED {}", maskAccountNumber(accountNumber));

        return accountNumber;
    }

    private Account saveWithUniqueNumber(Account account) {
        int attempts = 0;

        while (attempts < ATTEMPTS_FOR_GENERATION_ACC_NUMB) {
            try {
                return accountRepository.save(account);
            } catch (DataIntegrityViolationException e) {
                attempts++;

                log.warn("ACCOUNT_NUMBER_CONFLICT retry={}", attempts);

                account.setAccountNumber(generateAccountNumber());
            }
        }

        throw new RuntimeException("Failed to generate unique account number after retries");
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