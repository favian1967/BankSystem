package com.company.bank_system.controller;


import com.company.bank_system.dto.AdminAccountResponse;
import com.company.bank_system.dto.AdminCardResponse;
import com.company.bank_system.dto.AdminCreateAccountRequest;
import com.company.bank_system.dto.AdminCreateCardRequest;
import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.Card;
import com.company.bank_system.entity.User;
import com.company.bank_system.repo.AccountRepository;
import com.company.bank_system.repo.CardRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//ONLY FOR TESTS

@RestController
@RequestMapping("/api/admin")
@Profile("dev")
public class AdminUserController {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;

    public AdminUserController(CardRepository cardRepository, AccountRepository accountRepository) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
    }


    @GetMapping("/getAll")
    public List<AdminCardResponse> getAllCards(
            Authentication authentication
    ) {
        return cardRepository.findAll().stream()
                .map(c -> new AdminCardResponse(
                        c.getId(),
                        c.getCardNumber(),
                        c.getAccount().getId(),
                        c.getUser().getId()
                ))
                .toList();
    }

    @GetMapping("/cards/byUser/{userId}")
    public List<AdminCardResponse> findByUser(@PathVariable Long userId) {
        return cardRepository.findByUserId(userId).stream()
                .map(c -> new AdminCardResponse(
                        c.getId(),
                        c.getCardNumber(),
                        c.getAccount().getId(),
                        c.getUser().getId()
                ))
                .toList();
    }


    @PostMapping("/addCard")
    public AdminCardResponse createCard(@RequestBody AdminCreateCardRequest req) {
        Card card = new Card();
        card.setCardNumber(req.cardNumber());

        Account acc = new Account();
        acc.setId(req.accountId());
        card.setAccount(acc);

        User user = new User();
        user.setId(req.userId());
        card.setUser(user);

        Card saved = cardRepository.save(card);

        return new AdminCardResponse(
                saved.getId(),
                saved.getCardNumber(),
                saved.getAccount().getId(),
                saved.getUser().getId()
        );
    }


    @PostMapping("/addAccount")
    public AdminAccountResponse createAccount(@RequestBody AdminCreateAccountRequest req) {
        Account account = new Account();
        account.setAccountNumber(req.accountNumber());

        User user = new User();
        user.setId(req.userId());
        account.setUser(user);

        Account saved = accountRepository.save(account);

        return new AdminAccountResponse(
                saved.getId(),
                saved.getAccountNumber(),
                saved.getUser().getId()
        );
    }


}
