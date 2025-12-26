package com.company.bank_system.controller;


import com.company.bank_system.dto.CardResponse;
import com.company.bank_system.dto.CreateCardRequest;
import com.company.bank_system.service.CardService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {


    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/createCard")
    public CardResponse createCard(
            @RequestBody CreateCardRequest request
    ) {
        return cardService.createCard(request);
    }

    @GetMapping("/getMyCards")
    public List<CardResponse> getMyCards() { // ✅ Убрали параметр
        return cardService.getMyCards();
    }

    @GetMapping("/getCard/{id}")
    public CardResponse getCardById(
            @PathVariable Long id
    ) {
        return cardService.getCardById(id);
    }

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

}
