package com.company.bank_system.service;

import com.company.bank_system.dto.AccountResponse;
import com.company.bank_system.dto.CreateAccountRequest;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.Account.AccountStatus;
import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Currency;
import com.company.bank_system.entity.enums.User.UserRole;
import com.company.bank_system.exception.Exceptions.AccessDeniedException;
import com.company.bank_system.exception.Exceptions.AccountNotFoundException;
import com.company.bank_system.exception.Exceptions.InvalidOperationException;
import com.company.bank_system.repo.AccountRepository;
import com.company.bank_system.repo.UserRepository;
import com.company.bank_system.dto.UserCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @InjectMocks
    private AccountService accountService;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserRepository userRepository;

    private User user;
    private UserCache userCache;
    private Account account;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@test.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole(UserRole.USER);
        user.setConfirmed(true);

        userCache = new UserCache(1L, "test@test.com", "ROLE_USER", true, null);

        account = new Account();
        account.setId(1L);
        account.setUser(user);
        account.setAccountNumber("9999999912345678");
        account.setAccountType(AccountType.CHECKING);
        account.setCurrency(Currency.USD);
        account.setBalance(new BigDecimal("1000.00"));
        account.setStatus(AccountStatus.ACTIVE);
        account.setCreatedAt(LocalDateTime.now());
    }

    // ==================== CREATE ACCOUNT ====================

    @Test
    public void createAccount_shouldCreateSuccessfully() throws Exception {
        // ARRANGE
        CreateAccountRequest request = new CreateAccountRequest(
                AccountType.CHECKING,
                Currency.USD
        );

        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(accountRepository.existsByAccountNumber(any())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // ACT
        AccountResponse response = accountService.createAccount(request);

        // ASSERT
        assertNotNull(response);
        assertEquals(AccountType.CHECKING, response.accountType());
        assertEquals(Currency.USD, response.currency());
        assertEquals(BigDecimal.ZERO, response.balance());
        assertEquals(AccountStatus.ACTIVE, response.status());

        verify(accountRepository, times(1)).save(any(Account.class));
        verify(currentUserService, times(1)).getCurrentUser();
    }

    @Test
    public void createAccount_shouldFailWhenUserNotConfirmed() {
        // ARRANGE
        user.setConfirmed(false);
        CreateAccountRequest request = new CreateAccountRequest(
                AccountType.CHECKING,
                Currency.USD
        );

        when(currentUserService.getCurrentUser()).thenReturn(userCache);

        // ACT & ASSERT
        assertThrows(Exception.class, () -> accountService.createAccount(request));
        verify(accountRepository, never()).save(any());
    }

    // ==================== GET ACCOUNT BY ID ====================

    @Test
    public void getAccountById_shouldReturnAccount() {
        // ARRANGE
        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        // ACT
        AccountResponse response = accountService.getAccountById(1L);

        // ASSERT
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("9999999912345678", response.accountNumber());
        assertEquals(AccountType.CHECKING, response.accountType());
    }

    @Test
    public void getAccountById_shouldThrowWhenNotFound() {
        // ARRANGE
        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThrows(AccountNotFoundException.class, () -> accountService.getAccountById(999L));
    }

    @Test
    public void getAccountById_shouldThrowWhenAccessDenied() {
        // ARRANGE
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@test.com");

        UserCache otherUserCache = new UserCache(2L, "other@test.com", "ROLE_USER", true, null);
        when(currentUserService.getCurrentUser()).thenReturn(otherUserCache);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        // ACT & ASSERT
        assertThrows(AccessDeniedException.class, () -> accountService.getAccountById(1L));
    }

    // ==================== GET MY ACCOUNTS ====================

    @Test
    public void getMyAccounts_shouldReturnUserAccounts() {
        // ARRANGE
        Account account2 = new Account();
        account2.setId(2L);
        account2.setUser(user);
        account2.setAccountNumber("9999999987654321");
        account2.setAccountType(AccountType.SAVED);
        account2.setCurrency(Currency.RUB);
        account2.setBalance(new BigDecimal("5000.00"));
        account2.setStatus(AccountStatus.ACTIVE);

        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(accountRepository.findByUserId(1L)).thenReturn(List.of(account, account2));

        // ACT
        List<AccountResponse> responses = accountService.getMyAccounts();

        // ASSERT
        assertEquals(2, responses.size());
        verify(accountRepository, times(1)).findByUserId(1L);
    }

    @Test
    public void getMyAccounts_shouldReturnEmptyListWhenNoAccounts() {
        // ARRANGE
        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(accountRepository.findByUserId(1L)).thenReturn(List.of());

        // ACT
        List<AccountResponse> responses = accountService.getMyAccounts();

        // ASSERT
        assertTrue(responses.isEmpty());
    }

    // ==================== UPDATE STATUS ====================

    @Test
    public void updateAccountStatus_shouldUpdateSuccessfully() {
        // ARRANGE
        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        // ACT
        AccountResponse response = accountService.updateAccountStatus(1L, AccountStatus.BLOCKED);

        // ASSERT
        assertEquals(AccountStatus.BLOCKED, account.getStatus());
        assertNotNull(account.getUpdatedAt());
        verify(accountRepository, times(1)).save(account);
    }

    @Test
    public void updateAccountStatus_shouldFailWhenAccountIsClosed() {
        // ARRANGE
        account.setStatus(AccountStatus.CLOSED);

        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        // ACT & ASSERT
        assertThrows(InvalidOperationException.class,
                () -> accountService.updateAccountStatus(1L, AccountStatus.ACTIVE));

        verify(accountRepository, never()).save(any());
    }

    // ==================== CLOSE ACCOUNT ====================

    @Test
    public void closeAccount_shouldCloseSuccessfully() {
        // ARRANGE
        account.setBalance(BigDecimal.ZERO);

        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenReturn(account);

        // ACT
        accountService.closeAccount(1L);

        // ASSERT
        assertEquals(AccountStatus.CLOSED, account.getStatus());
        verify(accountRepository, times(1)).save(account);
    }

    @Test
    public void closeAccount_shouldFailWithNonZeroBalance() {
        // ARRANGE — account.balance = 1000
        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        // ACT & ASSERT
        assertThrows(InvalidOperationException.class,
                () -> accountService.closeAccount(1L));

        verify(accountRepository, never()).save(any());
    }

    @Test
    public void closeAccount_shouldFailWhenAlreadyClosed() {
        // ARRANGE
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.CLOSED);

        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        // ACT & ASSERT
        assertThrows(InvalidOperationException.class,
                () -> accountService.closeAccount(1L));
    }

    // ==================== GET BALANCE ====================

    @Test
    public void getAccountBalance_shouldReturnBalance() {
        // ARRANGE
        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        // ACT
        BigDecimal balance = accountService.getAccountBalance(1L);

        // ASSERT
        assertEquals(new BigDecimal("1000.00"), balance);
    }

    // ==================== TOTAL BALANCE ====================

    @Test
    public void getTotalBalanceByCurrency_shouldSumActiveAccounts() {
        // ARRANGE
        Account account2 = new Account();
        account2.setId(2L);
        account2.setUser(user);
        account2.setAccountType(AccountType.SAVED);
        account2.setCurrency(Currency.USD);
        account2.setBalance(new BigDecimal("500.00"));
        account2.setStatus(AccountStatus.ACTIVE);

        Account closedAccount = new Account();
        closedAccount.setId(3L);
        closedAccount.setUser(user);
        closedAccount.setAccountType(AccountType.DEPOSIT);
        closedAccount.setCurrency(Currency.USD);
        closedAccount.setBalance(new BigDecimal("200.00"));
        closedAccount.setStatus(AccountStatus.CLOSED);

        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(accountRepository.findByUserIdAndCurrency(1L, Currency.USD))
                .thenReturn(List.of(account, account2, closedAccount));

        // ACT
        BigDecimal total = accountService.getTotalBalanceByCurrency(Currency.USD);

        // ASSERT — only ACTIVE accounts: 1000 + 500 = 1500
        assertEquals(new BigDecimal("1500.00"), total);
    }

    // ==================== ACCOUNTS COUNT ====================

    @Test
    public void getAccountsCount_shouldReturnCount() {
        // ARRANGE
        when(currentUserService.getCurrentUser()).thenReturn(userCache);
        when(accountRepository.countByUserId(1L)).thenReturn(3L);

        // ACT
        long count = accountService.getAccountsCount();

        // ASSERT
        assertEquals(3L, count);
    }

    // ==================== ACCOUNT EXISTS ====================

    @Test
    public void accountExists_shouldReturnTrueWhenExists() {
        // ARRANGE
        when(accountRepository.existsByAccountNumber("9999999912345678")).thenReturn(true);

        // ACT & ASSERT
        assertTrue(accountService.accountExists("9999999912345678"));
    }

    @Test
    public void accountExists_shouldReturnFalseWhenNotExists() {
        // ARRANGE
        when(accountRepository.existsByAccountNumber("0000000000000000")).thenReturn(false);

        // ACT & ASSERT
        assertFalse(accountService.accountExists("0000000000000000"));
    }
}
