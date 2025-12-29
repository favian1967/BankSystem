package com.company.bank_system.service;

import com.company.bank_system.dto.CardResponse;
import com.company.bank_system.dto.CreateCardRequest;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.Card;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.Cards.CardStatus;
import com.company.bank_system.entity.enums.Cards.CardType;
import com.company.bank_system.entity.enums.User.UserRole;
import com.company.bank_system.exception.Exceptions.AccessDeniedException;
import com.company.bank_system.exception.Exceptions.CardAlreadyBlockedException;
import com.company.bank_system.exception.Exceptions.CardNotFoundException;
import com.company.bank_system.repo.CardRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public List<CardResponse> getCardsByAccount(Long accountId) {
        User user = currentUserService.getCurrentUser();
        Account account = accountService.getAccountEntityById(accountId);

        log.debug("GET_CARDS_BY_ACCOUNT userId={} accountId={}", user.getId(), accountId);

        List<Card> cards = cardRepository.findByAccount(account);

        log.info("GET_CARDS_BY_ACCOUNT_SUCCESS userId={} accountId={} count={}",
                user.getId(), accountId, cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CardResponse> getCardsByStatus(CardStatus status) {
        User user = currentUserService.getCurrentUser();

        log.debug("GET_CARDS_BY_STATUS userId={} status={}", user.getId(), status);

        List<Card> cards = cardRepository.findByUserAndStatus(user, status);

        log.info("GET_CARDS_BY_STATUS_SUCCESS userId={} status={} count={}",
                user.getId(), status, cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CardResponse> getCardsByType(CardType type) {
        User user = currentUserService.getCurrentUser();

        log.debug("GET_CARDS_BY_TYPE userId={} type={}", user.getId(), type);

        List<Card> cards = cardRepository.findByUserAndCardType(user, type);

        log.info("GET_CARDS_BY_TYPE_SUCCESS userId={} type={} count={}",
                user.getId(), type, cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CardResponse> getActiveCards() {
        User user = currentUserService.getCurrentUser();

        log.debug("GET_ACTIVE_CARDS userId={}", user.getId());

        List<Card> cards = cardRepository.findByUserAndStatus(user, CardStatus.ACTIVE);

        log.info("GET_ACTIVE_CARDS_SUCCESS userId={} count={}",
                user.getId(), cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CardResponse> getBlockedCards() {
        User user = currentUserService.getCurrentUser();

        log.debug("GET_BLOCKED_CARDS userId={}", user.getId());

        List<Card> cards = cardRepository.findByUserAndStatus(user, CardStatus.BLOCKED);

        log.info("GET_BLOCKED_CARDS_SUCCESS userId={} count={}",
                user.getId(), cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CardResponse> getExpiredCards() {
        User user = currentUserService.getCurrentUser();

        log.debug("GET_EXPIRED_CARDS userId={}", user.getId());

        List<Card> cards = cardRepository.findByUserAndExpiryDateBefore(user, LocalDate.now());

        log.info("GET_EXPIRED_CARDS_SUCCESS userId={} count={}",
                user.getId(), cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public long getCardsCount() {
        User user = currentUserService.getCurrentUser();

        log.debug("GET_CARDS_COUNT userId={}", user.getId());

        long count = cardRepository.countByUser(user);

        log.info("GET_CARDS_COUNT_SUCCESS userId={} count={}",
                user.getId(), count
        );

        return count;
    }

    public long getCardsCountByStatus(CardStatus status) {
        User user = currentUserService.getCurrentUser();

        log.debug("GET_CARDS_COUNT_BY_STATUS userId={} status={}",
                user.getId(), status
        );

        long count = cardRepository.countByUserAndStatus(user, status);

        log.info("GET_CARDS_COUNT_BY_STATUS_SUCCESS userId={} status={} count={}",
                user.getId(), status, count
        );

        return count;
    }

    @Transactional
    public void deleteCard(Long cardId) {
        User user = currentUserService.getCurrentUser();

        log.info("DELETE_CARD_START userId={} cardId={}", user.getId(), cardId);

        Card card = getCardEntityById(cardId);

        cardRepository.delete(card);

        log.info("DELETE_CARD_SUCCESS userId={} cardId={}", user.getId(), cardId);
    }

    public Map<String, Object> checkCardExpiry(Long cardId) {
        User user = currentUserService.getCurrentUser();

        log.debug("CHECK_CARD_EXPIRY userId={} cardId={}", user.getId(), cardId);

        Card card = getCardEntityById(cardId);

        boolean isExpired = card.getExpiryDate().isBefore(LocalDate.now());
        long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.now(),
                card.getExpiryDate()
        );

        log.info("CHECK_CARD_EXPIRY_SUCCESS cardId={} isExpired={} daysUntilExpiry={}",
                cardId, isExpired, daysUntilExpiry
        );

        Map<String, Object> result = new HashMap<>();
        result.put("cardId", cardId);
        result.put("isExpired", isExpired);
        result.put("expiryDate", card.getExpiryDate().toString());
        result.put("daysUntilExpiry", daysUntilExpiry);

        return result;
    }

    public List<CardResponse> adminGetAllCards() {
        User user = currentUserService.getCurrentUser();

        log.info("ADMIN_GET_ALL_CARDS adminId={}", user.getId());

        if (user.getRole() != UserRole.ADMIN) {
            log.error("ADMIN_GET_ALL_CARDS_ACCESS_DENIED userId={}", user.getId());
            throw new AccessDeniedException("Admin access required");
        }

        List<Card> cards = cardRepository.findAll();

        log.info("ADMIN_GET_ALL_CARDS_SUCCESS adminId={} count={}",
                user.getId(), cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Map<String, Object> adminGetCardStats() {
        User user = currentUserService.getCurrentUser();

        log.info("ADMIN_GET_CARD_STATS adminId={}", user.getId());

        if (user.getRole() != UserRole.ADMIN) {
            log.error("ADMIN_GET_CARD_STATS_ACCESS_DENIED userId={}", user.getId());
            throw new AccessDeniedException("Admin access required");
        }

        long totalCards = cardRepository.count();
        long activeCards = cardRepository.countByStatus(CardStatus.ACTIVE);
        long blockedCards = cardRepository.countByStatus(CardStatus.BLOCKED);
        long expiredCards = cardRepository.countByExpiryDateBefore(LocalDate.now());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCards", totalCards);
        stats.put("activeCards", activeCards);
        stats.put("blockedCards", blockedCards);
        stats.put("expiredCards", expiredCards);

        log.info("ADMIN_GET_CARD_STATS_SUCCESS adminId={} total={} active={} blocked={} expired={}",
                user.getId(), totalCards, activeCards, blockedCards, expiredCards
        );

        return stats;
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
