package com.company.bank_system.controller;
import com.company.bank_system.dto.DepositRequest;
import com.company.bank_system.dto.TransferRequest;
import com.company.bank_system.dto.WithdrawRequest;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.Account.AccountStatus;
import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Currency;
import com.company.bank_system.entity.enums.User.UserRole;
import com.company.bank_system.entity.enums.User.UserStatus;
import com.company.bank_system.repo.AccountRepository;
import com.company.bank_system.repo.IdempotentRepository;
import com.company.bank_system.repo.TransactionRepository;
import com.company.bank_system.repo.UserRepository;
import com.company.bank_system.service.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerTest {

    private final MockMvc mockMvc;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final IdempotentRepository idempotentRepository;


    @Autowired
    TransactionControllerTest(
            MockMvc mockMvc,
            AccountRepository accountRepository,
            UserRepository userRepository,
            TransactionRepository transactionRepository,
            JWTService jwtService,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper, IdempotentRepository idempotentRepository
    ) {
        this.mockMvc = mockMvc;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.idempotentRepository = idempotentRepository;
    }

    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    private User testUser;
    private User secondUser;
    private Account testAccount;
    private Account secondAccount;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        idempotentRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(UserRole.USER);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setPhone("+79505551234");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setConfirmed(true);
        testUser = userRepository.save(testUser);

        secondUser = new User();
        secondUser.setEmail("second@example.com");
        secondUser.setPasswordHash(passwordEncoder.encode("password123"));
        secondUser.setFirstName("Jane");
        secondUser.setLastName("Smith");
        secondUser.setRole(UserRole.USER);
        secondUser.setCreatedAt(LocalDateTime.now());
        secondUser.setPhone("+79505551235");
        secondUser.setStatus(UserStatus.ACTIVE);
        secondUser.setConfirmed(true);
        secondUser = userRepository.save(secondUser);

        testAccount = new Account();
        testAccount.setUser(testUser);
        testAccount.setAccountType(AccountType.CHECKING);
        testAccount.setCurrency(Currency.USD);
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setAccountNumber("40817840123456789012");
        testAccount.setCreatedAt(LocalDateTime.now());
        testAccount = accountRepository.save(testAccount);

        secondAccount = new Account();
        secondAccount.setUser(secondUser);
        secondAccount.setAccountType(AccountType.CHECKING);
        secondAccount.setCurrency(Currency.USD);
        secondAccount.setBalance(new BigDecimal("500.00"));
        secondAccount.setStatus(AccountStatus.ACTIVE);
        secondAccount.setAccountNumber("40817840987654321098");
        secondAccount.setCreatedAt(LocalDateTime.now());
        secondAccount = accountRepository.save(secondAccount);

        jwtToken = jwtService.generateToken(testUser.getEmail());
    }

    @Test
    void deposit_shouldDepositMoneySuccessfully() throws Exception {
        // ARRANGE
        DepositRequest request = new DepositRequest(
                testAccount.getId(),
                new BigDecimal("500.00"),
                "Test deposit"
        );

        BigDecimal expectedBalance = testAccount.getBalance().add(request.amount());

        // ACT & ASSERT
        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("Idempotency-Key", "test-key-5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.toAccountId").value(testAccount.getId()))
                .andExpect(jsonPath("$.fromAccountId").doesNotExist());

        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(expectedBalance);

        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void deposit_shouldFailWithInvalidAmount() throws Exception {
        // ARRANGE
        DepositRequest request = new DepositRequest(
                testAccount.getId(),
                new BigDecimal("-100.00"),
                "Invalid deposit"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("Idempotency-Key", "test-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        Account unchangedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertThat(unchangedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void withdraw_shouldWithdrawMoneySuccessfully() throws Exception {
        // ARRANGE
        WithdrawRequest request = new WithdrawRequest(
                testAccount.getId(),
                new BigDecimal("300.00"),
                "Test withdrawal"
        );

        BigDecimal expectedBalance = testAccount.getBalance().subtract(request.amount());

        // ACT & ASSERT
        mockMvc.perform(post("/api/transactions/withdraw")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("Idempotency-Key", "test-key-6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.transactionType").value("WITHDRAW"))
                .andExpect(jsonPath("$.amount").value(300.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.fromAccountId").value(testAccount.getId()))
                .andExpect(jsonPath("$.toAccountId").doesNotExist());

        Account updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo(expectedBalance);
    }

    @Test
    void withdraw_shouldFailWithInsufficientFunds() throws Exception {
        // ARRANGE
        WithdrawRequest request = new WithdrawRequest(
                testAccount.getId(),
                new BigDecimal("2000.00"),
                "Insufficient funds"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/transactions/withdraw")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("Idempotency-Key", "test-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        Account unchangedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertThat(unchangedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void withdraw_shouldFailWithInvalidAmount() throws Exception {
        // ARRANGE
        WithdrawRequest request = new WithdrawRequest(
                testAccount.getId(),
                BigDecimal.ZERO,
                "Invalid amount"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/transactions/withdraw")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("Idempotency-Key", "test-key-23")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void transfer_shouldTransferMoneyBetweenAccountsSuccessfully() throws Exception {
        // ARRANGE
        TransferRequest request = new TransferRequest(
                testAccount.getId(),
                secondAccount.getAccountNumber(),
                new BigDecimal("200.00"),
                "Test transfer"
        );

        BigDecimal expectedFromBalance = testAccount.getBalance().subtract(request.amount());
        BigDecimal expectedToBalance = secondAccount.getBalance().add(request.amount());

        // ACT & ASSERT
        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("Idempotency-Key", "test-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.fromAccountId").value(testAccount.getId()))
                .andExpect(jsonPath("$.toAccountId").value(secondAccount.getId()));

        Account updatedFromAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        Account updatedToAccount = accountRepository.findById(secondAccount.getId()).orElseThrow();

        assertThat(updatedFromAccount.getBalance()).isEqualByComparingTo(expectedFromBalance);
        assertThat(updatedToAccount.getBalance()).isEqualByComparingTo(expectedToBalance);
    }

    @Test
    void transfer_shouldFailWithInsufficientFunds() throws Exception {
        // ARRANGE
        TransferRequest request = new TransferRequest(
                testAccount.getId(),
                secondAccount.getAccountNumber(),
                new BigDecimal("2000.00"),
                "Insufficient funds transfer"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("Idempotency-Key", "test-key-7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());

        Account unchangedFromAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        Account unchangedToAccount = accountRepository.findById(secondAccount.getId()).orElseThrow();

        assertThat(unchangedFromAccount.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(unchangedToAccount.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void transfer_shouldFailWithCurrencyMismatch() throws Exception {
        // ARRANGE
        Account eurAccount = new Account();
        eurAccount.setUser(secondUser);
        eurAccount.setAccountType(AccountType.CHECKING);
        eurAccount.setCurrency(Currency.EUR);
        eurAccount.setBalance(new BigDecimal("500.00"));
        eurAccount.setStatus(AccountStatus.ACTIVE);
        eurAccount.setAccountNumber("40817978123456789012");
        eurAccount.setCreatedAt(LocalDateTime.now());
        eurAccount = accountRepository.save(eurAccount);

        TransferRequest request = new TransferRequest(
                testAccount.getId(),
                eurAccount.getAccountNumber(),
                new BigDecimal("100.00"),
                "Currency mismatch transfer"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("Idempotency-Key", "test-key-4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void transfer_shouldFailWhenTransferringToSameAccount() throws Exception {
        // ARRANGE
        TransferRequest request = new TransferRequest(
                testAccount.getId(),
                testAccount.getAccountNumber(),
                new BigDecimal("100.00"),
                "Transfer to same account"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("Idempotency-Key", "test-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void transfer_shouldFailWithInvalidAmount() throws Exception {
        // ARRANGE
        TransferRequest request = new TransferRequest(
                testAccount.getId(),
                secondAccount.getAccountNumber(),
                new BigDecimal("-50.00"),
                "Invalid transfer amount"
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + jwtToken)
                        .header("Idempotency-Key", "test-key-22")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }
}