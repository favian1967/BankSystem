package com.company.bank_system.service;

import com.company.bank_system.entity.Card;
import com.company.bank_system.repo.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

public class CardServiceTest {
    private CardService cardService;
    private CardRepository  cardRepository;


    @BeforeEach
    public void setUp() {
        cardRepository = mock(CardRepository.class);
        cardService = new CardService(null, null, null, null);
    }

    @Test
    public void shouldReturn(){

    }

}