package com.company.bank_system.repo;

import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.Account.AccountStatus;
import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUser(User user);

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByUserAndAccountType(User user, AccountType accountType);

    List<Account> findByUserAndCurrency(User user, Currency currency);

    List<Account> findByUserAndStatus(User user, AccountStatus status);

    @Query("SELECT a FROM Account a WHERE a.user = :user AND a.status = 'ACTIVE'")
    List<Account> findActiveAccountsByUser(@Param("user") User user);

    List<Account> findByUserAndAccountTypeAndCurrency(User user, AccountType accountType, Currency currency);

    long countByUser(User user);

    long countByUserAndStatus(User user, AccountStatus status);

    @Query("SELECT a FROM Account a WHERE a.user = :user AND a.balance > :minBalance")
    List<Account> findAccountsWithBalanceGreaterThan(@Param("user") User user, @Param("minBalance") java.math.BigDecimal minBalance);

    @Query("SELECT a FROM Account a WHERE a.user = :user AND a.status = 'BLOCKED'")
    List<Account> findBlockedAccounts(@Param("user") User user);

    @Query("SELECT a FROM Account a WHERE a.user = :user AND a.status = 'CLOSED'")
    List<Account> findClosedAccounts(@Param("user") User user);
}