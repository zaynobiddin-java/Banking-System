package com.bankingsystem.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.bankingsystem.dto.CardTransferRequest;
import com.bankingsystem.dto.DepositRequest;
import com.bankingsystem.dto.PaymentRequest;
import com.bankingsystem.dto.TransactionResponse;
import com.bankingsystem.dto.WithdrawRequest;
import com.bankingsystem.service.TransactionService;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/card-transfer")
    public ResponseEntity<TransactionResponse> cardTransfer(@Valid @RequestBody CardTransferRequest request) {
        return ResponseEntity.ok(transactionService.cardTransfer(request));
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(transactionService.deposit(request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(transactionService.withdraw(request));
    }

    @PostMapping("/payment")
    public ResponseEntity<TransactionResponse> payment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(transactionService.payment(request));
    }

    @GetMapping("/history/{accountId}")
    public ResponseEntity<List<TransactionResponse>> history(@PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.getAccountHistory(accountId));
    }
}


