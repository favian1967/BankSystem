package com.company.bank_system.service;


import com.company.bank_system.dto.DepositRequest;
import com.company.bank_system.dto.TransferRequest;
import com.company.bank_system.dto.WithdrawRequest;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.Transaction;
import com.company.bank_system.entity.enums.Currency;
import com.company.bank_system.entity.enums.Transaction.TransactionStatus;
import com.company.bank_system.entity.enums.Transaction.TransactionType;
import com.company.bank_system.repo.AccountRepository;
import com.company.bank_system.repo.TransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
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
    public Transaction deposit(DepositRequest depositRequest) {

        Account account = accountService.getAccountEntityById(depositRequest.accountId());

        if (depositRequest.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Сумма должна быть больше 0");
        }

        BigDecimal newBalance = account.getBalance().add(depositRequest.amount());
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setToAccount(account);          // Куда пришли деньги
        transaction.setFromAccount(null);           // Откуда пришли (нет источника)
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setAmount(depositRequest.amount());
        transaction.setCurrency(account.getCurrency());
        transaction.setDescription(depositRequest.description());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setCompletedAt(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction withdraw(WithdrawRequest withdrawRequest) {
        Account account = accountService.getAccountEntityById(withdrawRequest.accountId());
        if (withdrawRequest.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(">0");
        }

        if(account.getBalance().compareTo(withdrawRequest.amount()) < 0){
            throw new RuntimeException("Haven't money :(");
        }

        BigDecimal newBalance = account.getBalance().subtract(withdrawRequest.amount());
        account.setBalance(newBalance);
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setToAccount(null);          // Куда пришли деньги
        transaction.setFromAccount(account);           // Откуда пришли (нет источника)
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setAmount(withdrawRequest.amount());
        transaction.setCurrency(account.getCurrency());
        transaction.setDescription(withdrawRequest.description());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setCompletedAt(LocalDateTime.now());

        return transactionRepository.save(transaction);
    }


    @Transactional
    public Transaction transfer(TransferRequest transferRequest) {
        Account fromAccount = accountService.getAccountEntityById(transferRequest.fromAccountId());
        Account toAccount = accountService.getAccountEntityById(transferRequest.toAccountId());


        // Нельзя перевести самому себе на тот же счёт
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new RuntimeException("Нельзя перевести на тот же счёт");
        }

        // Проверяем сумму
        if (transferRequest.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Сумма должна быть больше 0");
        }

        // Проверяем баланс
        if (fromAccount.getBalance().compareTo(transferRequest.amount()) < 0) {
            throw new RuntimeException("Недостаточно средств");
        }

        // Проверяем валюту (можно переводить только в той же валюте)
        if (!fromAccount.getCurrency().equals(toAccount.getCurrency())) {
            throw new RuntimeException("Валюты счетов не совпадают");
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


        return  transactionRepository.save(transaction);
    }


    public List<Transaction> getAccountTransactions(Long accountId){

        Account account = accountService.getAccountEntityById(accountId);

        // Находим все транзакции где этот счёт участвует
        return transactionRepository.findByFromAccountOrToAccount(account, account);

    }

}
