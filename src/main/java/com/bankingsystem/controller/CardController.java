package com.bankingsystem.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.bankingsystem.dto.CardCreateRequest;
import com.bankingsystem.dto.CardResponse;
import com.bankingsystem.service.CardService;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Card")
public class CardController {

    private final CardService cardService;

    @PostMapping("/account/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> createCard(
            @PathVariable Long accountId,
            @Valid @RequestBody CardCreateRequest request) {
        return ResponseEntity.ok(cardService.createCard(accountId, request));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<CardResponse>> getCardsByAccountId(@PathVariable Long accountId) {
        return ResponseEntity.ok(cardService.getCardsByAccountId(accountId));
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponse> getCardById(@PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.getCardById(cardId));
    }

    @PatchMapping("/{cardId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> blockCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.blockCard(cardId));
    }

    @PatchMapping("/{cardId}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardResponse> unblockCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.unblockCard(cardId));
    }
}


