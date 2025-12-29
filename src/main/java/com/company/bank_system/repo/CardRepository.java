package com.company.bank_system.repo;

import com.company.bank_system.entity.Account;
import com.company.bank_system.entity.Card;
import com.company.bank_system.entity.User;
import com.company.bank_system.entity.enums.Cards.CardStatus;
import com.company.bank_system.entity.enums.Cards.CardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findByCardNumber(String cardNumber);
    List<Card> findByAccount(Account account);
    List<Card> findByUser(User user);
    List<Card> findByUserId(Long userId);

    boolean existsByCardNumber(String cardNumber);
    boolean existsByIdAndUserId(Long cardId, Long userId);


    List<Card> findByUserAndStatus(User user, CardStatus status);

    List<Card> findByUserAndCardType(User user, CardType cardType);

    List<Card> findByUserAndExpiryDateBefore(User user, LocalDate date);

    long countByUser(User user);

    long countByUserAndStatus(User user, CardStatus status);

    long countByStatus(CardStatus status);

    long countByExpiryDateBefore(LocalDate date);
}