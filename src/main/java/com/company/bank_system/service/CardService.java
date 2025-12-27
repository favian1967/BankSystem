package com.company.bank_system.service;


import com.company.bank_system.dto.AccountResponse;
import com.company.bank_system.dto.CardResponse;
import com.company.bank_system.dto.CreateCardRequest;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.Card;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.Cards.CardStatus;
import com.company.bank_system.entity.enums.User.UserRole;
import com.company.bank_system.exception.Exceptions.AccessDeniedException;
import com.company.bank_system.exception.Exceptions.CardAlreadyBlockedException;
import com.company.bank_system.exception.Exceptions.CardNotFoundException;
import com.company.bank_system.repo.CardRepository;
import com.company.bank_system.repo.UserRepository;
import jakarta.persistence.Id;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final AccountService accountService;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public CardService(CardRepository cardRepository, AccountService accountService, CurrentUserService currentUserService, PasswordEncoder passwordEncoder) {
        this.cardRepository = cardRepository;
        this.accountService = accountService;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CardResponse createCard(CreateCardRequest createCardRequest) {
        Account account = accountService.getAccountEntityById(createCardRequest.accountId());
        User user  = currentUserService.getCurrentUser();

        Card card = new Card();

        if (!account.getUser().getId().equals(user.getId()) && user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("You cannot create card for this account");
        }

        card.setAccount(account);
        card.setUser(user);
        card.setCardNumber(generateCardNumber());
        card.setCardHolderName(user.getFirstName() + " " + user.getLastName());
        card.setCvvHash(generateCvvHash());
        card.setExpiryDate(LocalDate.now().plusYears(5));
        card.setCardType(createCardRequest.cardType());
        card.setPaymentSystem(createCardRequest.paymentSystem());
        card.setStatus(CardStatus.ACTIVE);
        card.setCreatedAt(LocalDateTime.now());
        card.setUpdatedAt(LocalDateTime.now());

        Card card1 = cardRepository.save(card);

        return mapToResponse(card1);
    }


    public List<CardResponse> getMyCards() {
        User currentUser = currentUserService.getCurrentUser();
        List<Card> cards = cardRepository.findByUser(currentUser);

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CardResponse> adminGetCardsByUser(Long userId) {
        User currentUser = currentUserService.getCurrentUser();
        List<Card> cards = cardRepository.findByUserId(userId);

        if(currentUser.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("You cannot get cards for this account");
        }

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }


    public CardResponse getCardById(Long cardId) {
        Card card = getCardEntityById(cardId);
        return mapToResponse(card);
    }

    public Card getCardEntityById(Long cardId) {
        User user =  currentUserService.getCurrentUser();

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        boolean isOwner = card.getUser().getId().equals(user.getId());
        boolean isAdmin = UserRole.ADMIN.equals(user.getRole());


        if (!isAdmin && !isOwner)
            throw new AccessDeniedException("You are not allowed to access this card");


        return card;

    }
    @Transactional
    public CardResponse blockCard(Long cardId) {
        Card card = getCardEntityById(cardId);

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new CardAlreadyBlockedException(cardId);
        }

        card.setStatus(CardStatus.BLOCKED);
        card.setUpdatedAt(LocalDateTime.now());


        return mapToResponse(cardRepository.save(card));
        }

    @Transactional
    public CardResponse unblockCard(Long cardId) {
        Card card = getCardEntityById(cardId);

        if (card.getStatus() == CardStatus.ACTIVE) {
            throw new IllegalStateException("Карта уже активна");
        }

        card.setStatus(CardStatus.ACTIVE);
        card.setUpdatedAt(LocalDateTime.now());

        Card saved = cardRepository.save(card);
        return mapToResponse(saved);
    }


    public BigDecimal getCardBalance(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));
        Account account = accountService.getAccountEntityById(card.getAccount().getId());
        User user =  currentUserService.getCurrentUser();


        boolean isOwner = card.getUser().getId().equals(user.getId());
        boolean isAdmin = UserRole.ADMIN.equals(user.getRole());

        if (!isOwner && !isAdmin)
            throw new AccessDeniedException("You are not allowed to access this card");

        return account.getBalance();
    }


    private String generateCardNumber() {
        String cardNumber;
        int attempts = 0;

        do {
            if (attempts++ > 10) {
                throw new RuntimeException("Не удалось сгенерировать уникальный номер карты");
            }

            // Генерируем 16 цифр
            long part1 = ThreadLocalRandom.current().nextLong(100000000L, 999999999L); // 9
            long part2 = ThreadLocalRandom.current().nextLong(10000000L, 99999999L);   // 7
            cardNumber = String.format("%09d%07d", part1, part2);

        } while (cardRepository.existsByCardNumber(cardNumber));

        return cardNumber;
    }

    private String generateCvvHash(){
        Random random = new  Random();
        StringBuilder result = new StringBuilder();

        for(int i = 0; i < 3; i++){
            result.append(random.nextInt(10));
        }

        return passwordEncoder.encode(result.toString());
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber.length() < 4) {
            return "****";
        }
        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }

    private CardResponse mapToResponse(Card card) {
        return new CardResponse(
                card.getId(),
                maskCardNumber(card.getCardNumber()),
                card.getCardHolderName(),
                card.getExpiryDate(),
                card.getCardType(),
                card.getPaymentSystem(),
                card.getStatus(),
                card.getAccount() != null ? card.getAccount().getId() : null,
                card.getCreatedAt()
        );
    }

}