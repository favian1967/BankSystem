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


    List<Card> findByUserIdAndStatus(Long userId, CardStatus status);

    List<Card> findByUserIdAndCardType(Long userId, CardType cardType);

    List<Card> findByUserIdAndExpiryDateBefore(Long userId, LocalDate date);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, CardStatus status);

    long countByStatus(CardStatus status);

    long countByExpiryDateBefore(LocalDate date);
}