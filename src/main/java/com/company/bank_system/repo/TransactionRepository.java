package com.company.bank_system.repo;

import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromAccountOrToAccount(Account fromAccount, Account toAccount);
//    List<Transaction> findByFromAccountOrToAccount(Account fromAccount, Account toAccount, Pageable pageable);

    Page<Transaction> findByFromAccountOrToAccount(Account from, Account to, Pageable pageable);
}
