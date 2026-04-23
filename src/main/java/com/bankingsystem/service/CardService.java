package com.bankingsystem.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.bankingsystem.dto.CardCreateRequest;
import com.bankingsystem.dto.CardResponse;
import com.bankingsystem.entity.Account;
import com.bankingsystem.entity.AccountStatus;
import com.bankingsystem.entity.Card;
import com.bankingsystem.entity.CardStatus;
import com.bankingsystem.entity.CardType;
import com.bankingsystem.exception.BadRequestException;
import com.bankingsystem.exception.NotFoundException;
import com.bankingsystem.repository.AccountRepository;
import com.bankingsystem.repository.CardRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final AccessControlService accessControlService;

    public CardResponse createCard(Long accountId, CardCreateRequest request) {
        accessControlService.requireAdmin();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Hisob topilmadi"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Faqat aktiv hisobga karta ochish mumkin");
        }

        if (request.getType() == null) {
            throw new BadRequestException("Karta turi ko'rsatilishi shart");
        }

        String cardNumber = generateCardNumber(request.getType());
        String expiry = LocalDate.now().plusYears(3)
                .format(DateTimeFormatter.ofPattern("MM/yy"));

        Card card = Card.builder()
                .cardNumber(cardNumber)
                .expiry(expiry)
                .account(account)
                .type(request.getType())
                .status(CardStatus.ACTIVE)
                .build();

        cardRepository.save(card);
        return toResponse(card);
    }

    public List<CardResponse> getCardsByAccountId(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Hisob topilmadi"));

        accessControlService.requireAccountAccess(account);

        return cardRepository.findByAccountId(accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CardResponse getCardById(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Karta topilmadi"));
        accessControlService.requireCardAccess(card);
        return toResponse(card);
    }

    public CardResponse blockCard(Long cardId) {
        accessControlService.requireAdmin();

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Karta topilmadi"));

        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new BadRequestException("Muddati tugagan kartani bloklash mumkin emas");
        }

        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
        return toResponse(card);
    }

    public CardResponse unblockCard(Long cardId) {
        accessControlService.requireAdmin();

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Karta topilmadi"));

        if (card.getStatus() != CardStatus.BLOCKED) {
            throw new BadRequestException("Faqat bloklangan kartani aktivlashtirish mumkin");
        }

        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
        return toResponse(card);
    }

    private CardResponse toResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .cardNumber(card.getCardNumber())
                .expiry(card.getExpiry())
                .type(card.getType().name())
                .status(card.getStatus().name())
                .accountId(card.getAccount().getId())
                .build();
    }

    private String generateCardNumber(CardType type) {
        String prefix = switch (type) {
            case UZCARD -> "8600";
            case HUMO -> "9860";
            case VISA -> "4278";
            case MASTERCARD -> "5425";
        };

        String cardNumber;
        do {
            StringBuilder sb = new StringBuilder(prefix);
            for (int i = 0; i < 12; i++) {
                sb.append(ThreadLocalRandom.current().nextInt(10));
            }
            cardNumber = sb.toString();
        } while (cardRepository.existsByCardNumber(cardNumber));

        return cardNumber;
    }
}


