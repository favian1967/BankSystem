package com.company.bank_system.controller;

import com.company.bank_system.dto.CardIssueResponse;
import com.company.bank_system.dto.CardResponse;
import com.company.bank_system.dto.CreateCardRequest;
import com.company.bank_system.entity.enums.Cards.CardStatus;
import com.company.bank_system.entity.enums.Cards.CardType;
import com.company.bank_system.service.CardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
@Validated
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    public ResponseEntity<CardIssueResponse> createCard(
            @Valid @RequestBody CreateCardRequest request
    ) {
        return ResponseEntity.ok(cardService.createCard(request));
    }

    @GetMapping
    public ResponseEntity<List<CardResponse>> getMyCards() {
        return ResponseEntity.ok(cardService.getMyCards());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCardById(
            @PathVariable @Positive(message = "Card ID must be positive") Long id
    ) {
        return ResponseEntity.ok(cardService.getCardById(id));
    }

    @PatchMapping("/block/{id}")
    public ResponseEntity<CardResponse> blockCard(
            @PathVariable @Positive(message = "Card ID must be positive") Long id
    ) {
        return ResponseEntity.ok(cardService.blockCard(id));
    }

    @PatchMapping("/unblock/{id}")
    public ResponseEntity<CardResponse> unblockCard(
            @PathVariable @Positive(message = "Card ID must be positive") Long id
    ) {
        return ResponseEntity.ok(cardService.unblockCard(id));
    }

    @GetMapping("/balance/{id}")
    public ResponseEntity<BigDecimal> getCardBalance(
            @PathVariable @Positive(message = "Card ID must be positive") Long id
    ) {
        return ResponseEntity.ok(cardService.getCardBalance(id));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<CardResponse>> getCardsByAccount(
            @PathVariable @Positive(message = "Account ID must be positive") Long accountId
    ) {
        return ResponseEntity.ok(cardService.getCardsByAccount(accountId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CardResponse>> getCardsByStatus(
            @PathVariable CardStatus status
    ) {
        return ResponseEntity.ok(cardService.getCardsByStatus(status));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<CardResponse>> getCardsByType(
            @PathVariable CardType type
    ) {
        return ResponseEntity.ok(cardService.getCardsByType(type));
    }

    @GetMapping("/active")
    public ResponseEntity<List<CardResponse>> getActiveCards() {
        return ResponseEntity.ok(cardService.getActiveCards());
    }

    @GetMapping("/blocked")
    public ResponseEntity<List<CardResponse>> getBlockedCards() {
        return ResponseEntity.ok(cardService.getBlockedCards());
    }

    @GetMapping("/expired")
    public ResponseEntity<List<CardResponse>> getExpiredCards() {
        return ResponseEntity.ok(cardService.getExpiredCards());
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCardsCount() {
        long count = cardService.getCardsCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/count/status/{status}")
    public ResponseEntity<Map<String, Long>> getCardsCountByStatus(
            @PathVariable CardStatus status
    ) {
        long count = cardService.getCardsCountByStatus(status);
        return ResponseEntity.ok(Map.of("count", count, "status", (long) status.ordinal()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(
            @PathVariable @Positive(message = "Card ID must be positive") Long id
    ) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/expiry")
    public ResponseEntity<Map<String, Object>> checkCardExpiry(
            @PathVariable @Positive(message = "Card ID must be positive") Long id
    ) {
        return ResponseEntity.ok(cardService.checkCardExpiry(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users/{userId}")
    public ResponseEntity<List<CardResponse>> adminGetCardsByUser(
            @PathVariable @Positive(message = "User ID must be positive") Long userId
    ) {
        return ResponseEntity.ok(cardService.adminGetCardsByUserId(userId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    public ResponseEntity<List<CardResponse>> adminGetAllCards() {
        return ResponseEntity.ok(cardService.adminGetAllCards());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Object>> adminGetCardStats() {
        return ResponseEntity.ok(cardService.adminGetCardStats());
    }
}