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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class CardService {


    private final CardRepository cardRepository;
    private final AccountService accountService;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public CardService(CardRepository cardRepository,
                       AccountService accountService,
                       CurrentUserService currentUserService,
                       PasswordEncoder passwordEncoder) {
        this.cardRepository = cardRepository;
        this.accountService = accountService;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        User user = currentUserService.getCurrentUser();
        Account account = accountService.getAccountEntityById(request.accountId());

        log.info("CARD_CREATE_START userId={} accountId={}",
                user.getId(), account.getId()
        );

        if (!account.getUser().getId().equals(user.getId())
                && user.getRole() != UserRole.ADMIN) {

            log.error("CARD_CREATE_ACCESS_DENIED userId={} accountId={}",
                    user.getId(), account.getId()
            );
            throw new AccessDeniedException("You cannot create card for this account");
        }

        Card card = new Card();
        card.setAccount(account);
        card.setUser(user);
        card.setCardNumber(generateCardNumber());
        card.setCardHolderName(user.getFirstName() + " " + user.getLastName());
        card.setCvvHash(generateCvvHash());
        card.setExpiryDate(LocalDate.now().plusYears(5));
        card.setCardType(request.cardType());
        card.setPaymentSystem(request.paymentSystem());
        card.setStatus(CardStatus.ACTIVE);
        card.setCreatedAt(LocalDateTime.now());
        card.setUpdatedAt(LocalDateTime.now());

        Card saved = cardRepository.save(card);

        log.info("CARD_CREATE_SUCCESS cardId={} userId={} cardNumber={}",
                saved.getId(),
                user.getId(),
                maskCardNumber(saved.getCardNumber())
        );

        return mapToResponse(saved);
    }

    public List<CardResponse> getMyCards() {
        User user = currentUserService.getCurrentUser();

        log.debug("GET_MY_CARDS userId={}", user.getId());

        List<Card> cards = cardRepository.findByUser(user);

        log.info("GET_MY_CARDS_SUCCESS userId={} count={}",
                user.getId(), cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CardResponse> adminGetCardsByUser(Long userId) {
        User currentUser = currentUserService.getCurrentUser();

        log.info("ADMIN_GET_CARDS_START adminId={} targetUserId={}",
                currentUser.getId(), userId
        );

        if (currentUser.getRole() != UserRole.ADMIN) {
            log.error("ADMIN_GET_CARDS_ACCESS_DENIED userId={}",
                    currentUser.getId()
            );
            throw new AccessDeniedException("You cannot get cards for this account");
        }

        List<Card> cards = cardRepository.findByUserId(userId);

        log.info("ADMIN_GET_CARDS_SUCCESS adminId={} count={}",
                currentUser.getId(), cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CardResponse getCardById(Long cardId) {
        log.debug("GET_CARD_BY_ID cardId={}", cardId);
        return mapToResponse(getCardEntityById(cardId));
    }

    public Card getCardEntityById(Long cardId) {
        User user = currentUserService.getCurrentUser();

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> {
                    log.warn("CARD_NOT_FOUND cardId={}", cardId);
                    return new CardNotFoundException(cardId);
                });

        boolean isOwner = card.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            log.error("CARD_ACCESS_DENIED userId={} cardId={}",
                    user.getId(), cardId
            );
            throw new AccessDeniedException("You are not allowed to access this card");
        }

        return card;
    }

    @Transactional
    public CardResponse blockCard(Long cardId) {
        log.info("CARD_BLOCK_START cardId={}", cardId);

        Card card = getCardEntityById(cardId);

        if (card.getStatus() == CardStatus.BLOCKED) {
            log.warn("CARD_ALREADY_BLOCKED cardId={}", cardId);
            throw new CardAlreadyBlockedException(cardId);
        }

        card.setStatus(CardStatus.BLOCKED);
        card.setUpdatedAt(LocalDateTime.now());

        log.info("CARD_BLOCK_SUCCESS cardId={}", cardId);

        return mapToResponse(cardRepository.save(card));
    }

    @Transactional
    public CardResponse unblockCard(Long cardId) {
        log.info("CARD_UNBLOCK_START cardId={}", cardId);

        Card card = getCardEntityById(cardId);

        if (card.getStatus() == CardStatus.ACTIVE) {
            log.warn("CARD_ALREADY_ACTIVE cardId={}", cardId);
            throw new IllegalStateException("Card already active");
        }

        card.setStatus(CardStatus.ACTIVE);
        card.setUpdatedAt(LocalDateTime.now());

        log.info("CARD_UNBLOCK_SUCCESS cardId={}", cardId);

        return mapToResponse(cardRepository.save(card));
    }

    public BigDecimal getCardBalance(Long cardId) {
        User user = currentUserService.getCurrentUser();

        log.debug("GET_CARD_BALANCE_START userId={} cardId={}",
                user.getId(), cardId
        );

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> {
                    log.warn("CARD_NOT_FOUND cardId={}", cardId);
                    return new CardNotFoundException(cardId);
                });

        boolean isOwner = card.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            log.error("CARD_BALANCE_ACCESS_DENIED userId={} cardId={}",
                    user.getId(), cardId
            );
            throw new AccessDeniedException("You are not allowed to access this card");
        }

        log.info("GET_CARD_BALANCE_SUCCESS cardId={}", cardId);

        return accountService
                .getAccountEntityById(card.getAccount().getId())
                .getBalance();
    }

    private String generateCardNumber() {
        String cardNumber;
        int attempts = 0;

        do {
            if (attempts++ > 10) {
                log.error("CARD_NUMBER_GENERATION_FAILED");
                throw new RuntimeException("Cannot generate unique card number");
            }

            long part1 = ThreadLocalRandom.current().nextLong(100000000L, 999999999L);
            long part2 = ThreadLocalRandom.current().nextLong(10000000L, 99999999L);
            cardNumber = String.format("%09d%07d", part1, part2);

        } while (cardRepository.existsByCardNumber(cardNumber));

        log.debug("CARD_NUMBER_GENERATED {}", maskCardNumber(cardNumber));

        return cardNumber;
    }

    private String generateCvvHash() {
        return passwordEncoder.encode(
                String.valueOf(ThreadLocalRandom.current().nextInt(100, 999))
        );
    }

    private String maskCardNumber(String cardNumber) {
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
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
