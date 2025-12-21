package com.company.bank_system.controller;


import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.Card;
import com.company.bank_system.repo.AccountRepository;
import com.company.bank_system.repo.CardRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;

    public AdminUserController(CardRepository cardRepository, AccountRepository accountRepository) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/hello")
    public String hello(
            Authentication authentication
    ) {
        String email = authentication.getName();
        return "Hello, " + email + "!";
    }

    @GetMapping("/getAll")
    public List<Card> getAllCards(
            Authentication authentication
    ) {
        return cardRepository.findAll();
    }

    @GetMapping
    public List<Card> findByUser(
            Authentication authentication,
            @RequestBody Long userId
    ) {
        return cardRepository.findByUserId(userId);
    }


    @PostMapping("/addCard")
    public Card createCard(
            @RequestBody Card card
    ){
        return cardRepository.save(card);
    }

    @PostMapping("/addAccount")
    public Account createAccount(
            @RequestBody Account account
    ){
        return accountRepository.save(account);
    }

}
