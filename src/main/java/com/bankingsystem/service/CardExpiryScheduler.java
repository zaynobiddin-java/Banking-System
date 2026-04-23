package com.bankingsystem.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.bankingsystem.entity.Card;
import com.bankingsystem.entity.CardStatus;
import com.bankingsystem.repository.CardRepository;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardExpiryScheduler {

    private final CardRepository cardRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM/yy");

    @Scheduled(cron = "0 0 0 * * *")
    public void expireCards() {
        YearMonth now = YearMonth.now();

        List<Card> cards = cardRepository.findByStatusNot(CardStatus.EXPIRED);

        int count = 0;
        for (Card card : cards) {
            YearMonth expiryDate = YearMonth.parse(card.getExpiry(), FORMATTER);
            if (expiryDate.isBefore(now)) {
                card.setStatus(CardStatus.EXPIRED);
                cardRepository.save(card);
                count++;
            }
        }

        if (count > 0) {
            log.info("{} ta karta expired qilindi", count);
        }
    }
}


