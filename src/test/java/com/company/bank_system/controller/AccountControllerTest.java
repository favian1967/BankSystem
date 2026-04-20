package com.company.bank_system.controller;

import com.company.bank_system.dto.CreateAccountRequest;

import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.company.bank_system.dto.UpdateAccountStatusRequest;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.Account.AccountStatus;
import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Currency;
import com.company.bank_system.entity.enums.User.UserRole;
import com.company.bank_system.entity.enums.User.UserStatus;
import com.company.bank_system.repo.AccountRepository;
import com.company.bank_system.repo.UserRepository;
import com.company.bank_system.service.JWTService;
import com.company.bank_system.service.TokenRevocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.cache.CacheManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import org.springframework.kafka.test.context.EmbeddedKafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"ai_messages", "bank_ai_answers"})

class AccountControllerTest {
    @MockitoBean(reset = MockReset.BEFORE)
    private TokenRevocationService tokenRevocationService;

    private final MockMvc mockMvc;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;


    @Autowired
    AccountControllerTest(MockMvc mockMvc, AccountRepository accountRepository, UserRepository userRepository, JWTService jwtService, PasswordEncoder passwordEncoder, ObjectMapper objectMapper, CacheManager cacheManager) {
        this.mockMvc = mockMvc;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;
    }

    @Container
    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("testdb");

    @Container
    private static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    private User testUser;
    private String jwtToken;



    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        when(tokenRevocationService.isRevoked(anyString())).thenReturn(false);
        accountRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(UserRole.USER);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setPhone("+79505551234");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setConfirmed(true);
        testUser = userRepository.save(testUser);
        jwtToken = jwtService.generateToken(testUser.getEmail(), testUser.getRole().name());
    }


    @Test
    void createAccount_shouldCreateAccountSuccessfully() throws Exception {

        //ARRANGE
        CreateAccountRequest request = new CreateAccountRequest(
                AccountType.CHECKING,
                Currency.USD
        );

        //ACT & ASSERTIONS
        mockMvc.perform(post("/api/accounts/add")
                .header("Authorization", "Bearer " + jwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.account_number").isString())
                .andExpect(jsonPath("$.account_type").value("CHECKING"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        List<Account> accounts = accountRepository.findByUser(testUser);
        assertThat(accounts).hasSize(1);

        Account savedAccount = accounts.getFirst();

        assertThat(savedAccount.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(savedAccount.getAccountType()).isEqualTo(AccountType.CHECKING);
        assertThat(savedAccount.getCurrency()).isEqualTo(Currency.USD);
        assertThat(savedAccount.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedAccount.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(savedAccount.getAccountNumber()).startsWith("99999999");
        assertThat(savedAccount.getAccountNumber()).hasSize(16);

    }



    @Test
    void updateAccountStatus_shouldSetStatusFromRequestSuccessfully() throws Exception {
        // ARRANGE
        UpdateAccountStatusRequest request = new UpdateAccountStatusRequest(
                AccountStatus.CLOSED
        );

        Account account = new Account();
        account.setUser(testUser);
        account.setAccountType(AccountType.CHECKING);
        account.setCurrency(Currency.USD);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setAccountNumber("9999999934567890");
        account.setCreatedAt(LocalDateTime.now());

        account = accountRepository.save(account);
        // ACT & ASSERT
        mockMvc.perform(patch("/api/accounts/"+ account.getId() + "/status")
                .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        Account updatedAccount = accountRepository.findById(account.getId()).orElseThrow() ;

        assertThat(updatedAccount.getStatus()).isEqualTo(AccountStatus.CLOSED);

    }
    @Test
    void closeAccount_shouldCloseAccountSuccessfully() throws Exception {
        // ARRANGE
        Account account = new Account();
        account.setUser(testUser);
        account.setAccountType(AccountType.CHECKING);
        account.setCurrency(Currency.USD);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setAccountNumber("999999991234567890");
        account.setCreatedAt(LocalDateTime.now());
        account = accountRepository.save(account);

        // ACT & ASSERT
        mockMvc.perform(delete("/api/accounts/" + account.getId() + "/close")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        Account closedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(closedAccount.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void closeAccount_shouldFailWhenAccountAlreadyClosed() throws Exception {
        // ARRANGE
        Account account = new Account();
        account.setUser(testUser);
        account.setAccountType(AccountType.CHECKING);
        account.setCurrency(Currency.USD);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.CLOSED);  // Уже закрыт
        account.setAccountNumber("999999991234567890");
        account.setCreatedAt(LocalDateTime.now());
        account = accountRepository.save(account);

        // ACT & ASSERT
        mockMvc.perform(delete("/api/accounts/" + account.getId() + "/close")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(jsonPath("$.message").value("Account is already closed"));
    }

    @Test
    void closeAccount_shouldFailWhenAccountNotFound() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(delete("/api/accounts/99999/close")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void closeAccount_shouldFailWhenAccountBelongsToAnotherUser() throws Exception {
        // ARRANGE
        User anotherUser = new User();
        anotherUser.setEmail("another@example.com");
        anotherUser.setPasswordHash(passwordEncoder.encode("password"));
        anotherUser.setFirstName("Another");
        anotherUser.setLastName("User");
        anotherUser.setRole(UserRole.USER);
        anotherUser.setPhone("+79505551235");
        anotherUser.setStatus(UserStatus.ACTIVE);
        anotherUser.setConfirmed(true);
        anotherUser.setCreatedAt(LocalDateTime.now());
        anotherUser = userRepository.save(anotherUser);

        Account account = new Account();
        account.setUser(anotherUser);
        account.setAccountType(AccountType.CHECKING);
        account.setCurrency(Currency.USD);
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        account.setAccountNumber("40817840123456789012");
        account.setCreatedAt(LocalDateTime.now());
        account = accountRepository.save(account);
        // ACT & ASSERT
        mockMvc.perform(delete("/api/accounts/" + account.getId() + "/close")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isForbidden());
    }
}
