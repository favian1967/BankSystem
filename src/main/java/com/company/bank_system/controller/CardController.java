package com.company.bank_system.controller;

import com.company.bank_system.dto.CardResponse;
import com.company.bank_system.dto.CreateCardRequest;
import com.company.bank_system.entity.enums.Cards.CardStatus;
import com.company.bank_system.entity.enums.Cards.CardType;
import com.company.bank_system.service.CardService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cards")
public class CardController {


    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/createCard")
    public CardResponse createCard(
            @Valid @RequestBody CreateCardRequest request
    ) {
        return cardService.createCard(request);
    }

    @GetMapping("/getMyCards")
    public List<CardResponse> getMyCards() {
        return cardService.getMyCards();
    }

    @GetMapping("/getCard/{id}")
    public CardResponse getCardById(
            @PathVariable Long id
    ) {
        return cardService.getCardById(id);
    }
    //    @PreAuthorize("@cardPermission.canBlock(#id, authentication)")
    @PostMapping("/block/{id}")
    public CardResponse blockCard(
            @PathVariable Long id
    ) {
        return cardService.blockCard(id);
    }

    @PostMapping("/unblock/{id}")
    public CardResponse unblockCard(
            @PathVariable Long id
    ) {
        return cardService.unblockCard(id);
    }

    @GetMapping("/balance/{id}")
    public BigDecimal getCardBalance(@PathVariable Long id) {
        return cardService.getCardBalance(id);
    }

    @GetMapping("/getByAccount/{accountId}")
    public List<CardResponse> getCardsByAccount(@PathVariable Long accountId) {
        return cardService.getCardsByAccount(accountId);
    }

    @GetMapping("/getByStatus/{status}")
    public List<CardResponse> getCardsByStatus(@PathVariable CardStatus status) {
        return cardService.getCardsByStatus(status);
    }

    @GetMapping("/getByType/{type}")
    public List<CardResponse> getCardsByType(@PathVariable CardType type) {
        return cardService.getCardsByType(type);
    }


    @GetMapping("/active")
    public List<CardResponse> getActiveCards() {
        return cardService.getActiveCards();
    }

    @GetMapping("/blocked")
    public List<CardResponse> getBlockedCards() {
        return cardService.getBlockedCards();
    }

    @GetMapping("/expired")
    public List<CardResponse> getExpiredCards() {
        return cardService.getExpiredCards();
    }

    @GetMapping("/count")
    public Map<String, Long> getCardsCount() {
        long count = cardService.getCardsCount();
        return Map.of("count", count);
    }

    @GetMapping("/countByStatus/{status}")
    public Map<String, Long> getCardsCountByStatus(@PathVariable CardStatus status) {
        long count = cardService.getCardsCountByStatus(status);
        return Map.of("count", count, "status", (long) status.ordinal());
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, String> deleteCard(@PathVariable Long id) {
        cardService.deleteCard(id);
        return Map.of("message", "Card deleted successfully", "cardId", id.toString());
    }

    @GetMapping("/checkExpiry/{id}")
    public Map<String, Object> checkCardExpiry(@PathVariable Long id) {
        return cardService.checkCardExpiry(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/getByUser/{userId}")
    public List<CardResponse> adminGetCardsByUser(@PathVariable Long userId) {
        return cardService.adminGetCardsByUser(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/getAllCards")
    public List<CardResponse> adminGetAllCards() {
        return cardService.adminGetAllCards();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/stats")
    public Map<String, Object> adminGetCardStats() {
        return cardService.adminGetCardStats();
    }
}