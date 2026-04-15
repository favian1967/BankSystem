package com.company.bank_system.service;

import com.company.bank_system.dto.*;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.IdempotentEntity;
import com.company.bank_system.entity.Transaction;
import com.company.bank_system.entity.enums.Account.AccountStatus;
import com.company.bank_system.entity.enums.Idempotent.IdempotentStatus;
import com.company.bank_system.entity.enums.Transaction.TransactionStatus;
import com.company.bank_system.entity.enums.Transaction.TransactionType;
import com.company.bank_system.exception.Exceptions.AccountOperationException;
import com.company.bank_system.exception.Exceptions.CurrencyMismatchException;
import com.company.bank_system.exception.Exceptions.IdempotentException;
import com.company.bank_system.exception.Exceptions.InsufficientFundsException;
import com.company.bank_system.exception.Exceptions.InvalidAmountException;
import com.company.bank_system.repo.AccountRepository;
import com.company.bank_system.repo.IdempotentRepository;
import com.company.bank_system.repo.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Service
@Slf4j
public class TransactionService {

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotentRepository idempotentRepository;
    private final CurrentUserService currentUserService;
    public TransactionService(AccountService accountService, AccountRepository accountRepository, TransactionRepository transactionRepository, IdempotentRepository idempotentRepository, CurrentUserService currentUserService) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.idempotentRepository = idempotentRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#root.target.getCurrentUserCacheKey()")
    public TransactionResponse deposit(DepositRequest depositRequest, String idemKey) {
        log.info("DEPOSIT_START accountId={} amount={}",
                depositRequest.accountId(),
                depositRequest.amount()
        );

        if (depositRequest.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("DEPOSIT_INVALID_AMOUNT accountId={} amount={}",
                    depositRequest.accountId(),
                    depositRequest.amount()
            );
            throw new InvalidAmountException("Deposit amount must be greater than 0");
        }

        IdempotentEntity idem;
        try {
            idem = new IdempotentEntity();
            idem.setAccount(accountRepository.findById(depositRequest.accountId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"))
            );
            idem.setIdempotencyKey(idemKey);
            idem.setStatus(IdempotentStatus.IN_PROGRESS);
            idem.setCreatedAt(LocalDateTime.now());
            idempotentRepository.saveAndFlush(idem);
        } catch (DataIntegrityViolationException e) {
            throw new IdempotentException(e.getMessage());
        }

        Account account = accountService.getAnyAccountByIdForUpdate(depositRequest.accountId());
        requireActiveAccount(account);

        BigDecimal newBalance = account.getBalance().add(depositRequest.amount());
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setToAccount(account);
        transaction.setFromAccount(null);
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setAmount(depositRequest.amount());
        transaction.setCurrency(account.getCurrency());
        transaction.setDescription(depositRequest.description());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setCompletedAt(LocalDateTime.now());

        Transaction saved = transactionRepository.save(transaction);

        log.info("DEPOSIT_SUCCESS transactionId={} accountId={} amount={} newBalance={}",
                saved.getId(),
                account.getId(),
                depositRequest.amount(),
                newBalance
        );
        idem.setStatus(IdempotentStatus.SUCCESS);
        idem.setTransactionId(saved.getId());
        idempotentRepository.save(idem);
        return mapToResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#root.target.getCurrentUserCacheKey()")
    public TransactionResponse withdraw(WithdrawRequest withdrawRequest, String idemKey) {
        log.info("WITHDRAW_START accountId={} amount={}",
                withdrawRequest.accountId(),
                withdrawRequest.amount()
        );

        if (withdrawRequest.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("WITHDRAW_INVALID_AMOUNT accountId={} amount={}",
                    withdrawRequest.accountId(),
                    withdrawRequest.amount()
            );
            throw new InvalidAmountException("Withdrawal amount must be greater than 0");
        }

        IdempotentEntity idem;
        try {
            idem = new IdempotentEntity();
            idem.setAccount(accountRepository.findById(withdrawRequest.accountId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"))
            );
            idem.setStatus(IdempotentStatus.IN_PROGRESS);
            idem.setIdempotencyKey(idemKey);
            idem.setCreatedAt(LocalDateTime.now());
            idempotentRepository.saveAndFlush(idem);
        } catch (DataIntegrityViolationException e) {
            throw new IdempotentException(e.getMessage());
        }

        Account account = accountService.getAnyAccountByIdForUpdate(withdrawRequest.accountId());
        requireActiveAccount(account);

        if (account.getBalance().compareTo(withdrawRequest.amount()) < 0) {
            log.warn("WITHDRAW_INSUFFICIENT_FUNDS accountId={} requested={} available={}",
                    account.getId(),
                    withdrawRequest.amount(),
                    account.getBalance()
            );
            throw new InsufficientFundsException(
                    account.getId(),
                    withdrawRequest.amount(),
                    account.getBalance()
            );
        }

        BigDecimal newBalance = account.getBalance().subtract(withdrawRequest.amount());
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setToAccount(null);
        transaction.setFromAccount(account);
        transaction.setTransactionType(TransactionType.WITHDRAW);
        transaction.setAmount(withdrawRequest.amount());
        transaction.setCurrency(account.getCurrency());
        transaction.setDescription(withdrawRequest.description());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setCompletedAt(LocalDateTime.now());

        Transaction saved = transactionRepository.save(transaction);

        log.info("WITHDRAW_SUCCESS transactionId={} accountId={} amount={} newBalance={}",
                saved.getId(),
                account.getId(),
                withdrawRequest.amount(),
                newBalance
        );
        idem.setStatus(IdempotentStatus.SUCCESS);
        idem.setTransactionId(saved.getId());
        idempotentRepository.save(idem);
        return mapToResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#root.target.getCurrentUserCacheKey()")
    public TransactionResponse transfer(TransferRequest transferRequest, String idemKey) {
        log.info("TRANSFER_START fromAccountId={} toAccountNumber={} amount={}",
                transferRequest.fromAccountId(),
                maskAccountNumber(transferRequest.toAccountId()),
                transferRequest.amount()
        );

        if (transferRequest.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("TRANSFER_INVALID_AMOUNT amount={}", transferRequest.amount());
            throw new InvalidAmountException("Transfer amount must be greater than 0");
        }

        Account toAccountRef = accountService.getAccountByNumber(transferRequest.toAccountId());

        if (transferRequest.fromAccountId().equals(toAccountRef.getId())) {
            log.error("TRANSFER_SAME_ACCOUNT accountId={}", transferRequest.fromAccountId());
            throw new InvalidAmountException("Cannot transfer to the same account");
        }

        IdempotentEntity idem;
        try {
            idem = new IdempotentEntity();
            idem.setAccount(accountRepository.findById(transferRequest.fromAccountId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"))
            );
            idem.setStatus(IdempotentStatus.IN_PROGRESS);
            idem.setIdempotencyKey(idemKey);
            idem.setCreatedAt(LocalDateTime.now());
            idempotentRepository.saveAndFlush(idem);
        } catch (DataIntegrityViolationException e) {
            throw new IdempotentException(e.getMessage());
        }

        Long firstId  = Math.min(transferRequest.fromAccountId(), toAccountRef.getId());
        Long secondId = Math.max(transferRequest.fromAccountId(), toAccountRef.getId());

        Account lockedFirst  = accountService.getAnyAccountByIdForUpdate(firstId);
        Account lockedSecond = accountService.getAnyAccountByIdForUpdate(secondId);

        Account fromAccount = lockedFirst.getId().equals(transferRequest.fromAccountId())
                ? lockedFirst : lockedSecond;
        Account toAccount   = lockedFirst.getId().equals(toAccountRef.getId())
                ? lockedFirst : lockedSecond;

        requireActiveAccount(fromAccount);
        requireActiveAccount(toAccount);

        if (fromAccount.getBalance().compareTo(transferRequest.amount()) < 0) {
            log.warn("TRANSFER_INSUFFICIENT_FUNDS accountId={} requested={} available={}",
                    fromAccount.getId(),
                    transferRequest.amount(),
                    fromAccount.getBalance()
            );
            throw new InsufficientFundsException(
                    fromAccount.getId(),
                    transferRequest.amount(),
                    fromAccount.getBalance()
            );
        }

        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            log.error("TRANSFER_CURRENCY_MISMATCH fromCurrency={} toCurrency={}",
                    fromAccount.getCurrency(),
                    toAccount.getCurrency()
            );
            throw new CurrencyMismatchException(
                    "Currency mismatch: from account has " + fromAccount.getCurrency() +
                            " but to account has " + toAccount.getCurrency()
            );
        }

        fromAccount.setBalance(fromAccount.getBalance().subtract(transferRequest.amount()));
        fromAccount.setUpdatedAt(LocalDateTime.now());

        toAccount.setBalance(toAccount.getBalance().add(transferRequest.amount()));
        toAccount.setUpdatedAt(LocalDateTime.now());

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction = new Transaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setAmount(transferRequest.amount());
        transaction.setCurrency(fromAccount.getCurrency());
        transaction.setDescription(transferRequest.description());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setCompletedAt(LocalDateTime.now());

        Transaction saved = transactionRepository.save(transaction);

        log.info("TRANSFER_SUCCESS transactionId={} fromAccountId={} toAccountId={} amount={}",
                saved.getId(),
                fromAccount.getId(),
                toAccount.getId(),
                transferRequest.amount()
        );
        idem.setStatus(IdempotentStatus.SUCCESS);
        idem.setTransactionId(saved.getId());
        idempotentRepository.save(idem);
        return mapToResponse(saved);
    }

    public Page<TransactionResponse> getAccountTransactions(Long accountId, int page, int size) {
        log.debug("GET_TRANSACTIONS accountId={}", accountId);

        Account account = accountService.getAccountEntityById(accountId);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );
        return transactionRepository
                .findByFromAccountOrToAccount(account, account, pageable)
                .map(this::mapToResponse);
    }

    public List<TransactionResponse> getRecentTransactions(Long accountId, int limit) {
        log.debug("GET_RECENT_TRANSACTIONS accountId={} limit={}", accountId, limit);
        int safeLimit = Math.min(Math.max(limit, 1), 50);

        Account account = accountService.getAccountEntityById(accountId);
        return transactionRepository
                .findByFromAccountOrToAccount(
                        account,
                        account,
                        PageRequest.of(0, safeLimit, Sort.by("createdAt").descending())
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private void requireActiveAccount(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            log.warn("ACCOUNT_NOT_ACTIVE accountId={} status={}",
                    account.getId(), account.getStatus());
            throw new AccountOperationException(account.getId(), account.getStatus());
        }
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getFromAccount() != null ? transaction.getFromAccount().getId() : null,
                transaction.getToAccount() != null ? transaction.getToAccount().getId() : null,
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getDescription(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }

    private String maskAccountNumber(String number) {
        if (number == null || number.length() < 6) return "****";
        return number.substring(0, 4) + "****" + number.substring(number.length() - 2);
    }


    @Scheduled(fixedRate = 86_400_000)
    @Transactional
    public void cleanupIdempotencyKeys(){
        idempotentRepository.deleteAllByCreatedAtBefore(
                LocalDateTime.now().minusDays(2)
        );
    }

    @SuppressWarnings("unused")
    public String getCurrentUserCacheKey() {
        return currentUserService.getCurrentEmail();
    }

}