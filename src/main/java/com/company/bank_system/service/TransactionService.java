package com.company.bank_system.service;

import com.company.bank_system.dto.*;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.Transaction;
import com.company.bank_system.entity.enums.Transaction.TransactionStatus;
import com.company.bank_system.entity.enums.Transaction.TransactionType;
import com.company.bank_system.exception.Exceptions.CurrencyMismatchException;
import com.company.bank_system.exception.Exceptions.InsufficientFundsException;
import com.company.bank_system.exception.Exceptions.InvalidAmountException;
import com.company.bank_system.repo.AccountRepository;
import com.company.bank_system.repo.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Service
@Slf4j
public class TransactionService {

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountService accountService, AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransactionResponse deposit(DepositRequest depositRequest) {
        log.info("DEPOSIT_START accountId={} amount={}",
                depositRequest.accountId(),
                depositRequest.amount()
        );

        Account account = accountService.getAccountEntityById(depositRequest.accountId());

        if (depositRequest.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("DEPOSIT_INVALID_AMOUNT accountId={} amount={}",
                    depositRequest.accountId(),
                    depositRequest.amount()
            );
            throw new InvalidAmountException("Deposit amount must be greater than 0");
        }

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

        return mapToResponse(saved);
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawRequest withdrawRequest) {
        log.info("WITHDRAW_START accountId={} amount={}",
                withdrawRequest.accountId(),
                withdrawRequest.amount()
        );

        Account account = accountService.getAccountEntityById(withdrawRequest.accountId());

        if (withdrawRequest.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("WITHDRAW_INVALID_AMOUNT accountId={} amount={}",
                    withdrawRequest.accountId(),
                    withdrawRequest.amount()
            );
            throw new InvalidAmountException("Withdrawal amount must be greater than 0");
        }

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

        return mapToResponse(saved);
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest transferRequest) throws CurrencyMismatchException {
        log.info("TRANSFER_START fromAccountId={} toAccountNumber={} amount={}",
                transferRequest.fromAccountId(),
                maskAccountNumber(transferRequest.toAccountId()),
                transferRequest.amount()
        );

        Account fromAccount = accountService.getAnyAccountById(transferRequest.fromAccountId());
        Account toAccount = accountService.getAccountByNumber(transferRequest.toAccountId());

        if (fromAccount.getId().equals(toAccount.getId())) {
            log.error("TRANSFER_SAME_ACCOUNT accountId={}", fromAccount.getId());
            throw new InvalidAmountException("Cannot transfer to the same account");
        }

        if (transferRequest.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("TRANSFER_INVALID_AMOUNT amount={}", transferRequest.amount());
            throw new InvalidAmountException("Transfer amount must be greater than 0");
        }

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

        return mapToResponse(saved);
    }

    public List<Transaction> getAccountTransactions(Long accountId) {
        log.debug("GET_TRANSACTIONS accountId={}", accountId);

        Account account = accountService.getAccountEntityById(accountId);
        List<Transaction> transactions = transactionRepository.findByFromAccountOrToAccount(account, account);

        log.info("GET_TRANSACTIONS_SUCCESS accountId={} count={}",
                accountId,
                transactions.size()
        );

        return transactions;
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
}