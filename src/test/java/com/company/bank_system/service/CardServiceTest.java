package com.company.bank_system.service;

import com.company.bank_system.dto.CardResponse;
import com.company.bank_system.dto.CreateCardRequest;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.Card;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.Account.AccountType;
import com.company.bank_system.entity.enums.Cards.CardPaymentSystem;
import com.company.bank_system.entity.enums.Cards.CardStatus;
import com.company.bank_system.entity.enums.Cards.CardType;
import com.company.bank_system.entity.enums.Currency;
import com.company.bank_system.entity.enums.User.UserRole;
import com.company.bank_system.repo.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CardServiceTest {

    @InjectMocks
    private CardService cardService;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private AccountService accountService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private User user1;
    private Account account1;
    private Card card1;

    @BeforeEach
    public void setUp() {
        user1 = new  User();
        user1.setId(1L);
        user1.setEmail("test@test.com");
        user1.setFirstName("John");
        user1.setLastName("Doe");
        user1.setRole(UserRole.USER);

        account1 = new Account();
        account1.setId(1L);
        account1.setUser(user1);
        account1.setAccountNumber("40817123456789012345");
        account1.setAccountType(AccountType.CHECKING);
        account1.setCurrency(Currency.RUB);
        account1.setBalance(BigDecimal.valueOf(10000));

        card1 = new Card();
        card1.setId(1L);
        card1.setAccount(account1);
        card1.setUser(user1);
        card1.setCardNumber("1234567890123456");
        card1.setCardHolderName("John Doe");
        card1.setCvvHash("hashed_cvv");
        card1.setExpiryDate(LocalDate.now().plusYears(5));
        card1.setCardType(CardType.DEBIT);
        card1.setPaymentSystem(CardPaymentSystem.VISA);
        card1.setStatus(CardStatus.ACTIVE);
        card1.setCreatedAt(LocalDateTime.now());
    }

    @Test
    public void createCard_shouldCreateCardSuccessfully(){
        //ARRANGE

        CreateCardRequest request = new CreateCardRequest(
                1L,
                CardType.DEBIT,
                CardPaymentSystem.VISA
        );

        Card savedCard = new Card();

        savedCard.setId(1L);
        savedCard.setAccount(account1);
        savedCard.setUser(user1);
        savedCard.setCardNumber("1234567890123456");
        savedCard.setCardHolderName("John Doe");
        savedCard.setCvvHash("hashed_cvv");
        savedCard.setExpiryDate(LocalDate.now().plusYears(5));
        savedCard.setCardType(CardType.DEBIT);
        savedCard.setPaymentSystem(CardPaymentSystem.VISA);
        savedCard.setStatus(CardStatus.ACTIVE);
        savedCard.setCreatedAt(LocalDateTime.now());

        when(currentUserService.getCurrentUser()).thenReturn(user1);
        when(accountService.getAccountEntityById(request.accountId())).thenReturn(account1);
        when(cardRepository.existsByCardNumber(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed_cvv");
        when(cardRepository.save(any(Card.class))).thenReturn(savedCard);

        //ACT
        CardResponse cardResponse = cardService.createCard(request);

        //ASSERT
        assertNotNull(cardResponse);
        assertEquals(1L, cardResponse.id());
        assertEquals("John Doe",  cardResponse.cardHolderName());
        assertEquals(CardPaymentSystem.VISA, cardResponse.paymentSystem());
        assertEquals(CardStatus.ACTIVE, cardResponse.cardStatus());

        String cardNumber = cardResponse.cardNumber();

        assertNotNull(cardNumber);
        assertTrue(
                cardNumber.matches("^\\*{4} \\*{4} \\*{4} \\d{4}$"),
                "Card number must be masked as **** **** **** 1234"
        );



        verify(currentUserService, times(1)).getCurrentUser();
        verify(cardRepository, times(1)).save(any(Card.class));
        verify(accountService, times(1)).getAccountEntityById(request.accountId());
        verify(cardRepository, times(1)).save(any(Card.class));

    }

    @Test
    public void blockCard_shouldBlockCardByID(){
        //ARRANGE
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card1));
        when(cardRepository.save(any(Card.class))).thenReturn(card1);
        when(currentUserService.getCurrentUser()).thenReturn(user1);
        //ACT
        CardResponse cardResponse = cardService.blockCard(1L);
        //ASSERT
        assertEquals(CardStatus.BLOCKED, card1.getStatus());
    }

}