package com.company.bank_system.service;

import com.company.bank_system.dto.CardIssueResponse;
import com.company.bank_system.dto.CardResponse;
import com.company.bank_system.dto.CreateCardRequest;
import com.company.bank_system.dto.UserCache;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.Card;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.Cards.CardStatus;
import com.company.bank_system.entity.enums.Cards.CardType;
import com.company.bank_system.entity.enums.User.UserRole;
import com.company.bank_system.exception.Exceptions.AccessDeniedException;
import com.company.bank_system.exception.Exceptions.CardAlreadyBlockedException;
import com.company.bank_system.exception.Exceptions.CardNotFoundException;
import com.company.bank_system.exception.Exceptions.UserNotFoundException;
import com.company.bank_system.repo.CardRepository;
import com.company.bank_system.repo.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class CardService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CardRepository cardRepository;
    private final AccountService accountService;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public CardService(CardRepository cardRepository,
                       AccountService accountService,
                       CurrentUserService currentUserService,
                       UserRepository userRepository) {
        this.cardRepository = cardRepository;
        this.accountService = accountService;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @Transactional
    @CacheEvict(value = "cards", key = "#root.target.getCurrentUserCacheKey()")
    public CardIssueResponse createCard(CreateCardRequest request) {
        UserCache currentUser = currentUserService.getCurrentUser();

        Account account = accountService.getAccountEntityById(request.accountId());

        log.info("CARD_CREATE_START userId={} accountId={}",
                currentUser.id(), account.getId()
        );

        validateCardAccess(currentUser, account);

        Card card = buildCard(request, currentUser, account);

        Card saved = cardRepository.save(card);

        log.info("CARD_CREATE_SUCCESS cardId={} userId={} cardNumber={}",
                saved.getId(),
                currentUser.id(),
                maskCardNumber(saved.getCardNumber())
        );

        return mapToIssueResponse(saved, card.getCardNumber());
    }


    private void validateCardAccess(UserCache user, Account account) {
        boolean isOwner = account.getUser().getId().equals(user.id());
        boolean isAdmin = UserRole.ADMIN.toString().equals(user.role());

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You cannot create card for this account");
        }
    }

    private Card buildCard(CreateCardRequest request, UserCache userCache, Account account) {

        User userRef = userRepository.getReferenceById(userCache.id());

        Card card = new Card();
        card.setAccount(account);
        card.setUser(userRef);
        card.setCardNumber(generateCardNumber());
        card.setCardHolderName(userRef.getFirstName() + " " + userRef.getLastName());
        card.setExpiryDate(LocalDate.now().plusYears(5));
        card.setCardType(request.cardType());
        card.setPaymentSystem(request.paymentSystem());
        card.setStatus(CardStatus.ACTIVE);
        card.setCreatedAt(LocalDateTime.now());
        card.setUpdatedAt(LocalDateTime.now());

        return card;
    }

    private CardIssueResponse mapToIssueResponse(Card card, String rawCardNumber) {
        String cvv = generateCvv();

        return new CardIssueResponse(
                card.getId(),
                maskCardNumber(rawCardNumber),
                card.getCardHolderName(),
                card.getExpiryDate(),
                card.getCardType(),
                card.getPaymentSystem(),
                card.getStatus(),
                card.getAccount() != null ? card.getAccount().getId() : null,
                cvv,
                card.getCreatedAt()
        );
    }

    @Cacheable(value = "cards", key = "#root.target.getCurrentUserCacheKey()")
    public List<CardResponse> getMyCards() {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.debug("GET_MY_CARDS userId={}", currentUser.id());

        List<Card> cards = cardRepository.findByUserId(currentUser.id());

        log.info("GET_MY_CARDS_SUCCESS userId={} count={}",
                currentUser.id(), cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CardResponse> adminGetCardsByUserId(Long userId) {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.info("ADMIN_GET_CARDS_START adminId={} targetUserId={}",
                currentUser.id(), userId
        );

        if (!currentUser.role().equals(UserRole.ADMIN.toString())) {
            log.error("ADMIN_GET_CARDS_ACCESS_DENIED userId={}",
                    currentUser.id()
            );
            throw new AccessDeniedException("You cannot get cards for this account");
        }

        List<Card> cards = cardRepository.findByUserId(userId);

        log.info("ADMIN_GET_CARDS_SUCCESS adminId={} count={}",
                currentUser.id(), cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public CardResponse getCardById(Long cardId) {
        log.debug("GET_CARD_BY_ID cardId={}", cardId);
        return mapToResponse(getCardEntityById(cardId));
    }

    @Transactional
    @CacheEvict(value = "cards", key = "#root.target.getCurrentUserCacheKey()")
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
    @CacheEvict(value = "cards", key = "#root.target.getCurrentUserCacheKey()")
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
        Card card = getCardEntityById(cardId);

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

            long part1 = RANDOM.nextLong(100_000_000L, 1_000_000_000L);
            long part2 = RANDOM.nextLong(1_000_000L, 10_000_000L);
            cardNumber = String.format("%09d%07d", part1, part2);

        } while (cardRepository.existsByCardNumber(cardNumber));

        log.debug("CARD_NUMBER_GENERATED {}", maskCardNumber(cardNumber));

        return cardNumber;
    }

    private String generateCvv() {
        return String.format("%03d", RANDOM.nextInt(1000));
    }

    public List<CardResponse> getCardsByAccount(Long accountId) {
        UserCache user = currentUserService.getCurrentUser();
        Account account = accountService.getAccountEntityById(accountId);

        log.debug("GET_CARDS_BY_ACCOUNT userId={} accountId={}", user.id(), accountId);

        List<Card> cards = cardRepository.findByAccount(account);

        log.info("GET_CARDS_BY_ACCOUNT_SUCCESS userId={} accountId={} count={}",
                user.id(), accountId, cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CardResponse> getCardsByStatus(CardStatus status) {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.debug("GET_CARDS_BY_STATUS userId={} status={}", currentUser.id(), status);

        List<Card> cards = cardRepository.findByUserIdAndStatus(currentUser.id(), status);

        log.info("GET_CARDS_BY_STATUS_SUCCESS userId={} status={} count={}",
                currentUser.id(), status, cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CardResponse> getCardsByType(CardType type) {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.debug("GET_CARDS_BY_TYPE userId={} type={}", currentUser.id(), type);

        List<Card> cards = cardRepository.findByUserIdAndCardType(currentUser.id(), type);

        log.info("GET_CARDS_BY_TYPE_SUCCESS userId={} type={} count={}",
                currentUser.id(), type, cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CardResponse> getActiveCards() {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.debug("GET_ACTIVE_CARDS userId={}", currentUser.id());

        List<Card> cards = cardRepository.findByUserIdAndStatus(currentUser.id(), CardStatus.ACTIVE);

        log.info("GET_ACTIVE_CARDS_SUCCESS userId={} count={}",
                currentUser.id(), cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CardResponse> getBlockedCards() {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.debug("GET_BLOCKED_CARDS userId={}", currentUser.id());

        List<Card> cards = cardRepository.findByUserIdAndStatus(currentUser.id(), CardStatus.BLOCKED);

        log.info("GET_BLOCKED_CARDS_SUCCESS userId={} count={}",
                currentUser.id(), cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<CardResponse> getExpiredCards() {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.debug("GET_EXPIRED_CARDS userId={}", currentUser.id());

        List<Card> cards = cardRepository.findByUserIdAndExpiryDateBefore(currentUser.id(), LocalDate.now());

        log.info("GET_EXPIRED_CARDS_SUCCESS userId={} count={}",
                currentUser.id(), cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public long getCardsCount() {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.debug("GET_CARDS_COUNT userId={}", currentUser.id());

        long count = cardRepository.countByUserId(currentUser.id());

        log.info("GET_CARDS_COUNT_SUCCESS userId={} count={}",
                currentUser.id(), count
        );

        return count;
    }

    public long getCardsCountByStatus(CardStatus status) {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.debug("GET_CARDS_COUNT_BY_STATUS userId={} status={}",
                currentUser.id(), status
        );

        long count = cardRepository.countByUserIdAndStatus(currentUser.id(), status);

        log.info("GET_CARDS_COUNT_BY_STATUS_SUCCESS userId={} status={} count={}",
                currentUser.id(), status, count
        );

        return count;
    }

    @Transactional
    @CacheEvict(value = "cards", key = "#root.target.getCurrentUserCacheKey()")
    public void deleteCard(Long cardId) {

        log.info("DELETE_CARD_START cardId={}", cardId);

        Card card = getCardEntityById(cardId);

        cardRepository.delete(card);

        log.info("DELETE_CARD_SUCCESS cardId={}", cardId);
    }

    public Map<String, Object> checkCardExpiry(Long cardId) {

        log.debug("CHECK_CARD_EXPIRY cardId={}", cardId);

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
        UserCache currentUser = currentUserService.getCurrentUser();

        log.info("ADMIN_GET_ALL_CARDS adminId={}", currentUser.id());

        boolean isAdmin = UserRole.ADMIN.toString().equals(currentUser.role());

        if (!isAdmin) {
            throw new AccessDeniedException("Admin access required");
        }

        List<Card> cards = cardRepository.findAll();

        log.info("ADMIN_GET_ALL_CARDS_SUCCESS adminId={} count={}",
                currentUser.id(), cards.size()
        );

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Map<String, Object> adminGetCardStats() {
        UserCache currentUser = currentUserService.getCurrentUser();

        log.info("ADMIN_GET_CARD_STATS adminId={}", currentUser.id());

        boolean isAdmin = UserRole.ADMIN.toString().equals(currentUser.role());

        if (!isAdmin) {
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
                currentUser.id(), totalCards, activeCards, blockedCards, expiredCards
        );

        return stats;
    }

    public Card getCardEntityById(Long cardId) {
        UserCache currentUser = currentUserService.getCurrentUser();

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> {
                    log.warn("CARD_NOT_FOUND cardId={}", cardId);
                    return new CardNotFoundException(cardId);
                });

        boolean isOwner = card.getUser().getId().equals(currentUser.id());
        boolean isAdmin = currentUser.role().equals(UserRole.ADMIN.toString());

        if (!isOwner && !isAdmin) {
            log.error("CARD_ACCESS_DENIED userId={} cardId={}",
                    currentUser.id(), cardId
            );
            throw new AccessDeniedException("You are not allowed to access this card");
        }

        return card;
    }

    private String maskCardNumber(String cardNumber) {
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    CardResponse mapToResponse(Card card) {
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

    @SuppressWarnings("unused")
    public String getCurrentUserCacheKey() {
        return currentUserService.getCurrentEmail();
    }
}
