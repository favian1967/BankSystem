package com.company.bank_system.controller;

import com.company.bank_system.dto.CreateCardRequest;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.Card;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.Account.AccountStatus;
import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Cards.CardPaymentSystem;
import com.company.bank_system.entity.enums.Cards.CardStatus;
import com.company.bank_system.entity.enums.Cards.CardType;
import com.company.bank_system.entity.enums.Currency;
import com.company.bank_system.entity.enums.User.UserRole;
import com.company.bank_system.entity.enums.User.UserStatus;
import com.company.bank_system.repo.AccountRepository;
import com.company.bank_system.repo.CardRepository;
import com.company.bank_system.repo.UserRepository;
import com.company.bank_system.service.JWTService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.kafka.test.context.EmbeddedKafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"ai_messages", "bank_ai_answers"})
class CardControllerTest {

    private final MockMvc mockMvc;
    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Autowired
    CardControllerTest(
            MockMvc mockMvc,
            CardRepository cardRepository,
            AccountRepository accountRepository,
            UserRepository userRepository,
            JWTService jwtService,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper
    ) {
        this.mockMvc = mockMvc;
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
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
    private User otherUser;
    private User adminUser;
    private Account testAccount;
    private Account otherAccount;
    private Card testCard;
    private String userToken;
    private String otherUserToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        testUser = createUser("test@example.com", "John", "Doe", "+79505551234", UserRole.USER);
        testAccount = createAccount(testUser, "40817840123456789012", new BigDecimal("1000.00"));
        testCard = createCard(testAccount, testUser, CardType.DEBIT, CardPaymentSystem.VISA);
        userToken = jwtService.generateToken(testUser.getEmail());

        otherUser = createUser("other@example.com", "Jane", "Smith", "+79505551235", UserRole.USER);
        otherAccount = createAccount(otherUser, "40817840987654321098", new BigDecimal("500.00"));
        otherUserToken = jwtService.generateToken(otherUser.getEmail());

        adminUser = createUser("admin@example.com", "Admin", "User", "+79505551236", UserRole.ADMIN);
        adminToken = jwtService.generateToken(adminUser.getEmail());
    }

    private User createUser(String email, String firstName, String lastName, String phone, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        user.setPhone(phone);
        user.setStatus(UserStatus.ACTIVE);
        user.setConfirmed(true);
        return userRepository.save(user);
    }

    private Account createAccount(User user, String accountNumber, BigDecimal balance) {
        Account account = new Account();
        account.setUser(user);
        account.setAccountType(AccountType.CHECKING);
        account.setCurrency(Currency.USD);
        account.setBalance(balance);
        account.setStatus(AccountStatus.ACTIVE);
        account.setAccountNumber(accountNumber);
        account.setCreatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    private Card createCard(Account account, User user, CardType cardType, CardPaymentSystem paymentSystem) {
        Card card = new Card();
        card.setAccount(account);
        card.setUser(user);
        card.setCardNumber("1234567890123456");
        card.setCardHolderName(user.getFirstName() + " " + user.getLastName());
        String cvv1 = "111";
        card.setExpiryDate(LocalDate.now().plusYears(5));
        card.setCardType(cardType);
        card.setPaymentSystem(paymentSystem);
        card.setStatus(CardStatus.ACTIVE);
        card.setCreatedAt(LocalDateTime.now());
        return cardRepository.save(card);
    }

    @Test
    void createCard_shouldCreateCardSuccessfully() throws Exception {
        // ARRANGE
        CreateCardRequest request = new CreateCardRequest(
                testAccount.getId(),
                CardType.CREDIT,
                CardPaymentSystem.MASTERCARD
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/cards/createCard")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.cardHolderName").value("John Doe"))
                .andExpect(jsonPath("$.cardType").value("CREDIT"))
                .andExpect(jsonPath("$.paymentSystem").value("MASTERCARD"))
                .andExpect(jsonPath("$.cardStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.accountId").value(testAccount.getId()))
                .andExpect(jsonPath("$.cardNumber").value(org.hamcrest.Matchers.matchesPattern("^\\*{4} \\*{4} \\*{4} \\d{4}$")));

        List<Card> cards = cardRepository.findByUser(testUser);
        assertThat(cards).hasSize(2);
    }

    @Test
    void createCard_shouldFailWhenCreatingForOtherUsersAccount() throws Exception {
        // ARRANGE
        CreateCardRequest request = new CreateCardRequest(
                otherAccount.getId(),
                CardType.DEBIT,
                CardPaymentSystem.VISA
        );

        // ACT & ASSERT
        mockMvc.perform(post("/api/cards/createCard")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        List<Card> cards = cardRepository.findByUser(otherUser);
        assertThat(cards).isEmpty();
    }

    @Test
    void blockCard_shouldBlockCardSuccessfully() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(post("/api/cards/block/" + testCard.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCard.getId()))
                .andExpect(jsonPath("$.cardStatus").value("BLOCKED"));

        Card blockedCard = cardRepository.findById(testCard.getId()).orElseThrow();
        assertThat(blockedCard.getStatus()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    void blockCard_shouldFailWhenBlockingAlreadyBlockedCard() throws Exception {
        // ARRANGE
        testCard.setStatus(CardStatus.BLOCKED);
        cardRepository.save(testCard);

        // ACT & ASSERT
        mockMvc.perform(post("/api/cards/block/" + testCard.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void blockCard_shouldFailWhenBlockingOtherUsersCard() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(post("/api/cards/block/" + testCard.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());

        Card unchangedCard = cardRepository.findById(testCard.getId()).orElseThrow();
        assertThat(unchangedCard.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void unblockCard_shouldUnblockCardSuccessfully() throws Exception {
        // ARRANGE
        testCard.setStatus(CardStatus.BLOCKED);
        cardRepository.save(testCard);

        // ACT & ASSERT
        mockMvc.perform(post("/api/cards/unblock/" + testCard.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCard.getId()))
                .andExpect(jsonPath("$.cardStatus").value("ACTIVE"));

        Card unblockedCard = cardRepository.findById(testCard.getId()).orElseThrow();
        assertThat(unblockedCard.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void getCardById_shouldReturnCard() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(get("/api/cards/getCard/" + testCard.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCard.getId()))
                .andExpect(jsonPath("$.cardHolderName").value("John Doe"))
                .andExpect(jsonPath("$.cardType").value("DEBIT"));
    }

    @Test
    void getCardById_shouldFailWhenAccessingOtherUsersCard() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(get("/api/cards/getCard/" + testCard.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCardBalance_shouldReturnAccountBalance() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(get("/api/cards/balance/" + testCard.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1000.00));
    }

    @Test
    void deleteCard_shouldDeleteCardSuccessfully() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(delete("/api/cards/delete/" + testCard.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        assertThat(cardRepository.findById(testCard.getId())).isEmpty();
    }

    @Test
    void deleteCard_shouldFailWhenDeletingOtherUsersCard() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(delete("/api/cards/delete/" + testCard.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());

        assertThat(cardRepository.findById(testCard.getId())).isPresent();
    }

    @Test
    void adminGetCardsByUserId_shouldReturnUserCards() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(get("/api/cards/admin/getByUserId/" + testUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cardHolderName").value("John Doe"));
    }

    @Test
    void adminGetCardsByUserId_shouldFailWhenNotAdmin() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(get("/api/cards/admin/getByUserId/" + otherUser.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }


    @Test
    void adminGetAllCards_shouldFailWhenNotAdmin() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(get("/api/cards/admin/getAllCards")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }


    @Test
    void checkCardExpiry_shouldReturnExpiryInfo() throws Exception {
        // ACT & ASSERT
        mockMvc.perform(get("/api/cards/checkExpiry/" + testCard.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value(testCard.getId()))
                .andExpect(jsonPath("$.isExpired").value(false))
                .andExpect(jsonPath("$.expiryDate").exists())
                .andExpect(jsonPath("$.daysUntilExpiry").isNumber());
    }
}