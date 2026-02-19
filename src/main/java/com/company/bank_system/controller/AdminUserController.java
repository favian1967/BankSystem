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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//ONLY FOR TESTS

@RestController
@RequestMapping("/api/admin")
@Profile("dev")
@Validated
public class AdminUserController {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;

    public AdminUserController(CardRepository cardRepository, AccountRepository accountRepository) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<AdminCardResponse>> getAllCards(
            Authentication authentication
    ) {
        return ResponseEntity.ok(cardRepository.findAll().stream()
                .map(c -> new AdminCardResponse(
                        c.getId(),
                        c.getCardNumber(),
                        c.getAccount().getId(),
                        c.getUser().getId()
                ))
                .toList());
    }

    @GetMapping("/cards/byUser/{userId}")
    public ResponseEntity<List<AdminCardResponse>> findByUser(
            @PathVariable @Positive(message = "User ID must be positive") Long userId
    ) {
        return ResponseEntity.ok(cardRepository.findByUserId(userId).stream()
                .map(c -> new AdminCardResponse(
                        c.getId(),
                        c.getCardNumber(),
                        c.getAccount().getId(),
                        c.getUser().getId()
                ))
                .toList());
    }

    @PostMapping("/addCard")
    public ResponseEntity<AdminCardResponse> createCard(@Valid @RequestBody AdminCreateCardRequest req) {
        Card card = new Card();
        card.setCardNumber(req.cardNumber());

        Account acc = new Account();
        acc.setId(req.accountId());
        card.setAccount(acc);

        User user = new User();
        user.setId(req.userId());
        card.setUser(user);

        Card saved = cardRepository.save(card);

        return ResponseEntity.ok(new AdminCardResponse(
                saved.getId(),
                saved.getCardNumber(),
                saved.getAccount().getId(),
                saved.getUser().getId()
        ));
    }

    @PostMapping("/addAccount")
    public ResponseEntity<AdminAccountResponse> createAccount(@Valid @RequestBody AdminCreateAccountRequest req) {
        Account account = new Account();
        account.setAccountNumber(req.accountNumber());

        User user = new User();
        user.setId(req.userId());
        account.setUser(user);

        Account saved = accountRepository.save(account);

        return ResponseEntity.ok(new AdminAccountResponse(
                saved.getId(),
                saved.getAccountNumber(),
                saved.getUser().getId()
        ));
    }
}
