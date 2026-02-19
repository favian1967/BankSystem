package com.company.bank_system.service;

import com.company.bank_system.dto.DepositRequest;
import com.company.bank_system.dto.TransactionResponse;
import com.company.bank_system.dto.TransferRequest;
import com.company.bank_system.dto.WithdrawRequest;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.IdempotentEntity;
import com.company.bank_system.entity.Transaction;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.Account.AccountStatus;
import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Currency;
import com.company.bank_system.entity.enums.Transaction.TransactionStatus;
import com.company.bank_system.entity.enums.Transaction.TransactionType;
import com.company.bank_system.entity.enums.User.UserRole;
import com.company.bank_system.exception.Exceptions.CurrencyMismatchException;
import com.company.bank_system.exception.Exceptions.InsufficientFundsException;
import com.company.bank_system.exception.Exceptions.InvalidAmountException;
import com.company.bank_system.repo.AccountRepository;
import com.company.bank_system.repo.IdempotentRepository;
import com.company.bank_system.repo.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTest {

    @InjectMocks
    private TransactionService transactionService;
    @Mock
    private AccountService accountService;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private IdempotentRepository idempotentRepository;

    private User user;
    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setFirstName("John");
        user.setRole(UserRole.USER);

        fromAccount = new Account();
        fromAccount.setId(1L);
        fromAccount.setUser(user);
        fromAccount.setAccountNumber("40817840123456789012");
        fromAccount.setAccountType(AccountType.CHECKING);
        fromAccount.setCurrency(Currency.USD);
        fromAccount.setBalance(new BigDecimal("1000.00"));
        fromAccount.setStatus(AccountStatus.ACTIVE);
        fromAccount.setCreatedAt(LocalDateTime.now());

        User user2 = new User();
        user2.setId(2L);
        user2.setEmail("other@test.com");

        toAccount = new Account();
        toAccount.setId(2L);
        toAccount.setUser(user2);
        toAccount.setAccountNumber("40817840987654321098");
        toAccount.setAccountType(AccountType.CHECKING);
        toAccount.setCurrency(Currency.USD);
        toAccount.setBalance(new BigDecimal("500.00"));
        toAccount.setStatus(AccountStatus.ACTIVE);
        toAccount.setCreatedAt(LocalDateTime.now());
    }

    // ==================== DEPOSIT ====================

    @Test
    public void deposit_shouldDepositSuccessfully() {
        // ARRANGE
        DepositRequest request = new DepositRequest(1L, new BigDecimal("500.00"), "Test deposit");

        when(accountService.getAccountEntityById(1L)).thenReturn(fromAccount);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(idempotentRepository.saveAndFlush(any(IdempotentEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenReturn(fromAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        // ACT
        TransactionResponse response = transactionService.deposit(request, "idem-key-1");

        // ASSERT
        assertNotNull(response);
        assertEquals(TransactionType.DEPOSIT, response.transactionType());
        assertEquals(new BigDecimal("500.00"), response.amount());
        assertEquals(TransactionStatus.COMPLETED, response.status());
        assertEquals(new BigDecimal("1500.00"), fromAccount.getBalance());

        verify(accountRepository, times(1)).save(fromAccount);
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    // ==================== WITHDRAW ====================

    @Test
    public void withdraw_shouldWithdrawSuccessfully() {
        // ARRANGE
        WithdrawRequest request = new WithdrawRequest(1L, new BigDecimal("300.00"), "Test withdrawal");

        when(accountService.getAccountEntityById(1L)).thenReturn(fromAccount);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(idempotentRepository.saveAndFlush(any(IdempotentEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenReturn(fromAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(2L);
            return t;
        });

        // ACT
        TransactionResponse response = transactionService.withdraw(request, "idem-key-2");

        // ASSERT
        assertNotNull(response);
        assertEquals(TransactionType.WITHDRAW, response.transactionType());
        assertEquals(new BigDecimal("300.00"), response.amount());
        assertEquals(new BigDecimal("700.00"), fromAccount.getBalance());

        verify(accountRepository, times(1)).save(fromAccount);
    }

    @Test
    public void withdraw_shouldFailWithInsufficientFunds() {
        // ARRANGE
        WithdrawRequest request = new WithdrawRequest(1L, new BigDecimal("2000.00"), "Too much");

        when(accountService.getAccountEntityById(1L)).thenReturn(fromAccount);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(idempotentRepository.saveAndFlush(any(IdempotentEntity.class))).thenAnswer(i -> i.getArgument(0));

        // ACT & ASSERT
        assertThrows(InsufficientFundsException.class,
                () -> transactionService.withdraw(request, "idem-key-3"));

        assertEquals(new BigDecimal("1000.00"), fromAccount.getBalance());
        verify(accountRepository, never()).save(any());
    }

    @Test
    public void withdraw_shouldFailWithZeroAmount() {
        // ARRANGE
        WithdrawRequest request = new WithdrawRequest(1L, BigDecimal.ZERO, "Zero");

        when(accountService.getAccountEntityById(1L)).thenReturn(fromAccount);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(idempotentRepository.saveAndFlush(any(IdempotentEntity.class))).thenAnswer(i -> i.getArgument(0));

        // ACT & ASSERT
        assertThrows(InvalidAmountException.class,
                () -> transactionService.withdraw(request, "idem-key-4"));
    }

    // ==================== TRANSFER ====================

    @Test
    public void transfer_shouldTransferSuccessfully() {
        // ARRANGE
        TransferRequest request = new TransferRequest(
                1L, "40817840987654321098", new BigDecimal("200.00"), "Transfer"
        );

        when(accountService.getAnyAccountById(1L)).thenReturn(fromAccount);
        when(accountService.getAccountByNumber("40817840987654321098")).thenReturn(toAccount);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(idempotentRepository.saveAndFlush(any(IdempotentEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(3L);
            return t;
        });

        // ACT
        TransactionResponse response = transactionService.transfer(request, "idem-key-5");

        // ASSERT
        assertNotNull(response);
        assertEquals(TransactionType.TRANSFER, response.transactionType());
        assertEquals(new BigDecimal("200.00"), response.amount());
        assertEquals(new BigDecimal("800.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), toAccount.getBalance());

        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(1)).save(argThat(t ->
                t.getTransactionType() == TransactionType.TRANSFER &&
                        t.getFromAccount().equals(fromAccount) &&
                        t.getToAccount().equals(toAccount)
        ));
    }

    @Test
    public void transfer_shouldFailWithInsufficientFunds() {
        // ARRANGE
        TransferRequest request = new TransferRequest(
                1L, "40817840987654321098", new BigDecimal("5000.00"), "Too much"
        );

        when(accountService.getAnyAccountById(1L)).thenReturn(fromAccount);
        when(accountService.getAccountByNumber("40817840987654321098")).thenReturn(toAccount);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(idempotentRepository.saveAndFlush(any(IdempotentEntity.class))).thenAnswer(i -> i.getArgument(0));

        // ACT & ASSERT
        assertThrows(InsufficientFundsException.class,
                () -> transactionService.transfer(request, "idem-key-6"));

        assertEquals(new BigDecimal("1000.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("500.00"), toAccount.getBalance());
    }

    @Test
    public void transfer_shouldFailWhenSameAccount() {
        // ARRANGE
        TransferRequest request = new TransferRequest(
                1L, "40817840123456789012", new BigDecimal("100.00"), "Same"
        );

        when(accountService.getAnyAccountById(1L)).thenReturn(fromAccount);
        when(accountService.getAccountByNumber("40817840123456789012")).thenReturn(fromAccount);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(idempotentRepository.saveAndFlush(any(IdempotentEntity.class))).thenAnswer(i -> i.getArgument(0));

        // ACT & ASSERT
        assertThrows(InvalidAmountException.class,
                () -> transactionService.transfer(request, "idem-key-7"));
    }

    @Test
    public void transfer_shouldFailWithCurrencyMismatch() {
        // ARRANGE
        toAccount.setCurrency(Currency.EUR);

        TransferRequest request = new TransferRequest(
                1L, "40817840987654321098", new BigDecimal("100.00"), "Mismatch"
        );

        when(accountService.getAnyAccountById(1L)).thenReturn(fromAccount);
        when(accountService.getAccountByNumber("40817840987654321098")).thenReturn(toAccount);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(fromAccount));
        when(idempotentRepository.saveAndFlush(any(IdempotentEntity.class))).thenAnswer(i -> i.getArgument(0));

        // ACT & ASSERT
        assertThrows(CurrencyMismatchException.class,
                () -> transactionService.transfer(request, "idem-key-8"));

        assertEquals(new BigDecimal("1000.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("500.00"), toAccount.getBalance());
    }
}
