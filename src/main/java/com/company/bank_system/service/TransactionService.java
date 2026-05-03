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
    @CacheEvict(value = "accounts", allEntries = true)
    public TransactionResponse deposit(DepositRequest req, String idemKey) {
        log.info("DEPOSIT_START accountId={} amount={}", req.accountId(), req.amount());

        validateAmount(req.amount(), "Deposit");

        IdempotentEntity idem = startIdempotent(req.accountId(), idemKey);

        Account account = accountService.getMyAccountByIdForUpdate(req.accountId());
        requireActiveAccount(account);

        increaseBalance(account, req.amount());
        accountRepository.save(account);

        Transaction saved = transactionRepository.save(
                Transaction.buildTransaction(null, account, TransactionType.DEPOSIT,
                        req.amount(), req.description(), account.getCurrency())
        );

        log.info("DEPOSIT_SUCCESS transactionId={} accountId={} amount={} newBalance={}",
                saved.getId(), account.getId(), req.amount(), account.getBalance());

        completeIdempotent(idem, saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public TransactionResponse withdraw(WithdrawRequest req, String idemKey) {
        log.info("WITHDRAW_START accountId={} amount={}", req.accountId(), req.amount());

        validateAmount(req.amount(), "Withdrawal");

        IdempotentEntity idem = startIdempotent(req.accountId(), idemKey);

        Account account = accountService.getMyAccountByIdForUpdate(req.accountId());
        requireActiveAccount(account);

        decreaseBalance(account, req.amount());
        accountRepository.save(account);

        Transaction saved = transactionRepository.save(
                Transaction.buildTransaction(account, null, TransactionType.WITHDRAW,
                        req.amount(), req.description(), account.getCurrency())
        );

        log.info("WITHDRAW_SUCCESS transactionId={} accountId={} amount={} newBalance={}",
                saved.getId(), account.getId(), req.amount(), account.getBalance());

        completeIdempotent(idem, saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "accounts", allEntries = true)
    public TransactionResponse transfer(TransferRequest req, String idemKey) {
        log.info("TRANSFER_START fromAccountId={} toAccountNumber={} amount={}",
                req.fromAccountId(), maskAccountNumber(req.toAccountId()), req.amount());

        validateAmount(req.amount(), "Transfer");

        Account toAccountRef = accountService.getAccountByNumber(req.toAccountId());

        if (req.fromAccountId().equals(toAccountRef.getId())) {
            throw new InvalidAmountException("Cannot transfer to the same account");
        }

        IdempotentEntity idem = startIdempotent(req.fromAccountId(), idemKey);

        Long firstId  = Math.min(req.fromAccountId(), toAccountRef.getId());
        Long secondId = Math.max(req.fromAccountId(), toAccountRef.getId());

        Account lockedFirst  = accountService.getAnyAccountByIdForUpdate(firstId);
        Account lockedSecond = accountService.getAnyAccountByIdForUpdate(secondId);

        Account fromAccount = lockedFirst.getId().equals(req.fromAccountId()) ? lockedFirst : lockedSecond;
        Account toAccount   = lockedFirst.getId().equals(toAccountRef.getId()) ? lockedFirst : lockedSecond;

        accountService.verifyOwnership(fromAccount);

        requireActiveAccount(fromAccount);
        requireActiveAccount(toAccount);

        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new CurrencyMismatchException("Currency mismatch: from=" +
                    fromAccount.getCurrency() + " to=" + toAccount.getCurrency());
        }

        decreaseBalance(fromAccount, req.amount());
        increaseBalance(toAccount, req.amount());

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction saved = transactionRepository.save(
                Transaction.buildTransaction(fromAccount, toAccount, TransactionType.TRANSFER,
                        req.amount(), req.description(), fromAccount.getCurrency())
        );

        log.info("TRANSFER_SUCCESS transactionId={} fromAccountId={} toAccountId={} amount={}",
                saved.getId(), fromAccount.getId(), toAccount.getId(), req.amount());

        completeIdempotent(idem, saved.getId());
        return mapToResponse(saved);
    }

    private IdempotentEntity startIdempotent(Long accountId, String key) {
        try {
            IdempotentEntity idem = new IdempotentEntity();
            idem.setAccount(accountRepository.getReferenceById(accountId));
            idem.setIdempotencyKey(key);
            idem.setStatus(IdempotentStatus.IN_PROGRESS);
            idem.setCreatedAt(LocalDateTime.now());

            return idempotentRepository.saveAndFlush(idem);

        } catch (DataIntegrityViolationException e) {
            throw new IdempotentException("Duplicate request");
        }
    }



    private void completeIdempotent(IdempotentEntity idem, Long txId) {
        idem.setStatus(IdempotentStatus.SUCCESS);
        idem.setTransactionId(txId);
        idempotentRepository.save(idem);
    }

    private void validateAmount(BigDecimal amount, String operation) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException(operation + " amount must be > 0");
        }
    }

    private void increaseBalance(Account acc, BigDecimal amount) {
        acc.setBalance(acc.getBalance().add(amount));
        acc.setUpdatedAt(LocalDateTime.now());
    }

    private void decreaseBalance(Account acc, BigDecimal amount) {
        if (acc.getBalance().compareTo(amount) < 0) {
            log.warn("INSUFFICIENT_FUNDS accountId={} requested={} available={}",
                    acc.getId(), amount, acc.getBalance());
            throw new InsufficientFundsException(acc.getId(), amount, acc.getBalance());
        }
        acc.setBalance(acc.getBalance().subtract(amount));
        acc.setUpdatedAt(LocalDateTime.now());
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